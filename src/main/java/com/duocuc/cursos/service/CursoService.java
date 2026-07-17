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
    public CursoDTO.GuiaResponse crearYSubirGuia(CursoDTO.CrearGuiaRequest request) throws IOException {
        // Generar número único
        String codigoCurso = "CURSO-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // Crear entidad
        Curso guia = new Curso();
        guia.setNumeroGuia(codigoCurso);
        guia.setTransportista(request.getTransportista());
        guia.setDestinatario(request.getDestinatario());
        guia.setDireccionDestino(request.getDireccionDestino());
        guia.setDescripcionCarga(request.getDescripcionCarga());
        guia.setPesoKg(request.getPesoKg());
        guia.setFechaDespacho(request.getFechaDespacho());
        guia.setEstado(Curso.EstadoGuia.PENDIENTE);
        guia = repository.save(guia);

        // Generar PDF
        byte[] pdfBytes = pdfService.generarPdf(guia);

        // Guardar en EFS (almacenamiento temporal)
        efsService.efsDisponible(); // Verifica/crea directorio base
        String rutaEfs = efsService.guardarEnEfs(pdfBytes, request.getTransportista(), codigoCurso);
        guia.setRutaEfs(rutaEfs);
        guia.setEstado(Curso.EstadoGuia.GENERADA);

        // Subir a S3 con carpetas por fecha/instructor
        String claveS3 = s3Service.construirClaveS3(
                request.getFechaDespacho(), request.getTransportista(), codigoCurso);
        s3Service.subirArchivo(pdfBytes, claveS3);
        guia.setClaveS3(claveS3);
        guia.setEstado(Curso.EstadoGuia.SUBIDA_S3);

        guia = repository.save(guia);
        // Semana 8: enviar guía a la Cola 1 (cursos-cola-inscripciones) para procesamiento asíncrono
        productorGuiasService.enviarGuia(guia);
        log.info("Guía creada y subida a S3: {} -> {}", codigoCurso, claveS3);
        return CursoDTO.GuiaResponse.from(guia);
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
    public CursoDTO.GuiaResponse actualizarGuia(String codigoCurso, CursoDTO.ActualizarGuiaRequest request) throws IOException {
        Curso guia = obtenerPorNumero(codigoCurso);

        // Actualizar campos si vienen en el request
        if (request.getDestinatario() != null)    guia.setDestinatario(request.getDestinatario());
        if (request.getDireccionDestino() != null) guia.setDireccionDestino(request.getDireccionDestino());
        if (request.getDescripcionCarga() != null) guia.setDescripcionCarga(request.getDescripcionCarga());
        if (request.getPesoKg() != null)           guia.setPesoKg(request.getPesoKg());
        if (request.getFechaDespacho() != null)    guia.setFechaDespacho(request.getFechaDespacho());
        if (request.getEstado() != null)           guia.setEstado(request.getEstado());

        guia = repository.save(guia);

        // Regenerar PDF con datos actualizados
        byte[] pdfActualizado = pdfService.generarPdf(guia);

        // Actualizar en EFS si existe
        if (guia.getRutaEfs() != null) {
            efsService.guardarEnEfs(pdfActualizado, guia.getTransportista(), guia.getNumeroGuia());
        }

        // Actualizar en S3 (sobreescribe el objeto existente)
        if (guia.getClaveS3() != null) {
            s3Service.actualizarArchivo(pdfActualizado, guia.getClaveS3());
        }

        log.info("Guía actualizada: {}", codigoCurso);
        return CursoDTO.GuiaResponse.from(guia);
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
    public List<CursoDTO.GuiaResponse> consultarHistorial(String instructor, LocalDate fecha) {
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
                .map(CursoDTO.GuiaResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una guía por su número.
     */
    public CursoDTO.GuiaResponse obtenerGuia(String codigoCurso) {
        return CursoDTO.GuiaResponse.from(obtenerPorNumero(codigoCurso));
    }

    private Curso obtenerPorNumero(String codigoCurso) {
        return repository.findByNumeroGuia(codigoCurso)
                .orElseThrow(() -> new RuntimeException("Guía no encontrada: " + codigoCurso));
    }
}
