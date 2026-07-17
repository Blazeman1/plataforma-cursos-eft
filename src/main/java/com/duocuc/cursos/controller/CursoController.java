package com.duocuc.cursos.controller;

import com.duocuc.cursos.dto.CursoDTO;
import com.duocuc.cursos.service.CursoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cursos")
@RequiredArgsConstructor
public class CursoController {

    private final CursoService service;

    /**
     * POST /api/cursos
     * Criterios 1 + 2: Crea la guía, la guarda en EFS y la sube automáticamente a S3.
     */
    @PostMapping
    public ResponseEntity<CursoDTO.GuiaResponse> crearGuia(
            @Valid @RequestBody CursoDTO.CrearGuiaRequest request) throws IOException {
        CursoDTO.GuiaResponse response = service.crearYSubirGuia(request);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * GET /api/cursos/{codigoCurso}
     * Obtiene los metadatos de una guía específica.
     */
    @GetMapping("/{codigoCurso}")
    public ResponseEntity<CursoDTO.GuiaResponse> obtenerGuia(
            @PathVariable String codigoCurso) {
        return ResponseEntity.ok(service.obtenerGuia(codigoCurso));
    }

    /**
     * GET /api/cursos/{codigoCurso}/descargar
     * Criterio 4: Descarga el PDF desde S3 con validación de existencia.
     */
    @GetMapping("/{codigoCurso}/descargar")
    public ResponseEntity<byte[]> descargarGuia(
            @PathVariable String codigoCurso) {
        byte[] pdf = service.descargarGuia(codigoCurso);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", codigoCurso + ".pdf");
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    /**
     * PUT /api/cursos/{codigoCurso}
     * Criterio 3: Modifica la guía y actualiza el PDF en S3.
     */
    @PutMapping("/{codigoCurso}")
    public ResponseEntity<CursoDTO.GuiaResponse> actualizarGuia(
            @PathVariable String codigoCurso,
            @RequestBody CursoDTO.ActualizarGuiaRequest request) throws IOException {
        return ResponseEntity.ok(service.actualizarGuia(codigoCurso, request));
    }

    /**
     * DELETE /api/cursos/{codigoCurso}
     * Elimina la guía de S3, EFS y base de datos.
     */
    @DeleteMapping("/{codigoCurso}")
    public ResponseEntity<Map<String, String>> eliminarGuia(
            @PathVariable String codigoCurso) {
        service.eliminarGuia(codigoCurso);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Guía eliminada correctamente",
                "codigoCurso", codigoCurso
        ));
    }

    /**
     * GET /api/cursos/historial
     * Criterio 5: Consulta el historial filtrado por instructor y/o fecha.
     *
     * Ejemplos:
     *   GET /api/cursos/historial?instructor=TransportesXYZ
     *   GET /api/cursos/historial?fecha=2024-01-15
     *   GET /api/cursos/historial?instructor=TransportesXYZ&fecha=2024-01-15
     *   GET /api/cursos/historial  (retorna todas)
     */
    @GetMapping("/historial")
    public ResponseEntity<List<CursoDTO.GuiaResponse>> consultarHistorial(
            @RequestParam(required = false) String instructor,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<CursoDTO.GuiaResponse> historial = service.consultarHistorial(instructor, fecha);
        return ResponseEntity.ok(historial);
    }

    /**
     * GET /api/cursos/health
     * Endpoint de salud para verificar que la aplicación está activa.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "estado", "UP",
                "servicio", "Sistema de Gestión de Cursos en Línea",
                "version", "1.0.0"
        ));
    }
}
