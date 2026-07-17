package com.duocuc.cursos.service;

import com.duocuc.cursos.dto.CursoDTO;
import com.duocuc.cursos.model.Curso;
import com.duocuc.cursos.repository.CursoRepository;
import com.duocuc.cursos.service.ProductorCursosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CursoService {

    private final CursoRepository repository;
    private final PdfService pdfService;
    private final EfsService efsService;
    private final S3Service s3Service;
    private final ProductorCursosService productorGuiasService;

    /**
     * Criterio 1 + 2: Crea la guía, genera el PDF, lo guarda en EFS y lo sube a S3.
     */
    @Transactional
    public CursoDTO.CursoResponse crearYSubirGuia(CursoDTO.CrearCursoRequest request) throws IOException {
        // Generar número único
        String codigoCurso = "CURSO-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // Crear entidad
        Curso guia = new Curso();
        guia.setCodigoCurso(codigoCurso);
        guia.setInstructor(request.getInstructor());
        guia.setEstudiante(request.getEstudiante());
        guia.setTematica(request.getTematica());
        guia.setDescripcion(request.getDescripcion());
        guia.setDuracionHoras(request.getDuracionHoras());
        guia.setFechaInicio(request.getFechaInicio());
        guia.setEstado(Curso.EstadoCurso.PENDIENTE);
        guia = repository.save(guia);

        // Generar PDF
        byte[] pdfBytes = pdfService.generarPdf(guia);

        // Guardar en EFS (almacenamiento temporal)
        efsService.efsDisponible(); // Verifica/crea directorio base
        String rutaEfs = efsService.guardarEnEfs(pdfBytes, request.getInstructor(), codigoCurso);
        guia.setRutaEfs(rutaEfs);
        guia.setEstado(Curso.EstadoCurso.GENERADA);

        // Subir a S3 con carpetas por fecha/instructor
        String claveS3 = s3Service.construirClaveS3(
                request.getFechaInicio(), request.getInstructor(), codigoCurso);
        s3Service.subirArchivo(pdfBytes, claveS3);
        guia.setClaveS3(claveS3);
        guia.setEstado(Curso.EstadoCurso.SUBIDA_S3);

        guia = repository.save(guia);
        // Semana 8: enviar guía a la Cola 1 (cursos-cola-inscripciones) para procesamiento asíncrono
        productorGuiasService.enviarGuia(guia);
        log.info("Guía creada y subida a S3: {} -> {}", codigoCurso, claveS3);
        return CursoDTO.CursoResponse.from(guia);
    }

    /**
     * Criterio 4: Descarga la guía desde S3.
     */
    public byte[] descargarGuia(String codigoCurso) {
        Curso guia = obtenerPorNumero(codigoCurso);
        if (guia.getClaveS3() == null) {
            throw new IllegalStateException("La guía " + codigoCurso + " no ha sido subida a S3 aún.");
        }
        return s3Service.descargarArchivo(guia.getClaveS3());
    }

    /**
     * Criterio 3: Modifica los datos de la guía y actualiza el archivo en S3.
     */
    @Transactional
    public CursoDTO.CursoResponse actualizarGuia(String codigoCurso, CursoDTO.ActualizarCursoRequest request) throws IOException {
        Curso guia = obtenerPorNumero(codigoCurso);

        // Actualizar campos si vienen en el request
        if (request.getEstudiante() != null)    guia.setEstudiante(request.getEstudiante());
        if (request.getTematica() != null) guia.setTematica(request.getTematica());
        if (request.getDescripcion() != null) guia.setDescripcion(request.getDescripcion());
        if (request.getDuracionHoras() != null)           guia.setDuracionHoras(request.getDuracionHoras());
        if (request.getFechaInicio() != null)    guia.setFechaInicio(request.getFechaInicio());
        if (request.getEstado() != null)           guia.setEstado(request.getEstado());

        guia = repository.save(guia);

        // Regenerar PDF con datos actualizados
        byte[] pdfActualizado = pdfService.generarPdf(guia);

        // Actualizar en EFS si existe
        if (guia.getRutaEfs() != null) {
            efsService.guardarEnEfs(pdfActualizado, guia.getInstructor(), guia.getCodigoCurso());
        }

        // Actualizar en S3 (sobreescribe el objeto existente)
        if (guia.getClaveS3() != null) {
            s3Service.actualizarArchivo(pdfActualizado, guia.getClaveS3());
        }

        log.info("Guía actualizada: {}", codigoCurso);
        return CursoDTO.CursoResponse.from(guia);
    }

    /**
     * Elimina la guía de S3, EFS y base de datos.
     */
    @Transactional
    public void eliminarGuia(String codigoCurso) {
        Curso guia = obtenerPorNumero(codigoCurso);

        // Eliminar de S3
        if (guia.getClaveS3() != null) {
            s3Service.eliminarArchivo(guia.getClaveS3());
        }

        // Eliminar del EFS
        if (guia.getRutaEfs() != null) {
            efsService.eliminarDeEfs(guia.getRutaEfs());
        }

        repository.delete(guia);
        log.info("Guía eliminada: {}", codigoCurso);
    }

    /**
     * Criterio 5: Consulta historial por instructor y/o fecha.
     */
    public List<CursoDTO.CursoResponse> consultarHistorial(String instructor, LocalDate fecha) {
        List<Curso> guias;

        if (instructor != null && fecha != null) {
            guias = repository.findByTransportistaAndFechaDespacho(instructor, fecha);
        } else if (instructor != null) {
            guias = repository.findByTransportista(instructor);
        } else if (fecha != null) {
            guias = repository.findByFechaDespacho(fecha);
        } else {
            guias = repository.findAll();
        }

        return guias.stream()
                .map(CursoDTO.CursoResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una guía por su número.
     */
    public CursoDTO.CursoResponse obtenerGuia(String codigoCurso) {
        return CursoDTO.CursoResponse.from(obtenerPorNumero(codigoCurso));
    }

    private Curso obtenerPorNumero(String codigoCurso) {
        return repository.findByNumeroGuia(codigoCurso)
                .orElseThrow(() -> new RuntimeException("Guía no encontrada: " + codigoCurso));
    }
}
