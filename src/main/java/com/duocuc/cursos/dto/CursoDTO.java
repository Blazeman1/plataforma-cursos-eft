package com.duocuc.cursos.dto;

import com.duocuc.cursos.model.Curso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CursoDTO {

    @Data
    public static class CrearGuiaRequest {
        @NotBlank(message = "El instructor es obligatorio")
        private String instructor;

        @NotBlank(message = "El estudiante es obligatorio")
        private String estudiante;

        @NotBlank(message = "La dirección de destino es obligatoria")
        private String tematica;

        @NotBlank(message = "La descripción de la carga es obligatoria")
        private String descripcion;

        private Double duracionHoras;

        @NotNull(message = "La fecha de despacho es obligatoria")
        private LocalDate fechaInicio;
    }

    @Data
    public static class ActualizarGuiaRequest {
        private String estudiante;
        private String tematica;
        private String descripcion;
        private Double duracionHoras;
        private LocalDate fechaInicio;
        private Curso.EstadoGuia estado;
    }

    @Data
    public static class GuiaResponse {
        private Long id;
        private String codigoCurso;
        private String instructor;
        private String estudiante;
        private String tematica;
        private String descripcion;
        private Double duracionHoras;
        private LocalDate fechaInicio;
        private Curso.EstadoGuia estado;
        private String rutaEfs;
        private String claveS3;
        private LocalDateTime fechaCreacion;
        private LocalDateTime fechaActualizacion;

        public static GuiaResponse from(Curso guia) {
            GuiaResponse r = new GuiaResponse();
            r.setId(guia.getId());
            r.setNumeroGuia(guia.getNumeroGuia());
            r.setTransportista(guia.getTransportista());
            r.setDestinatario(guia.getDestinatario());
            r.setDireccionDestino(guia.getDireccionDestino());
            r.setDescripcionCarga(guia.getDescripcionCarga());
            r.setPesoKg(guia.getPesoKg());
            r.setFechaDespacho(guia.getFechaDespacho());
            r.setEstado(guia.getEstado());
            r.setRutaEfs(guia.getRutaEfs());
            r.setClaveS3(guia.getClaveS3());
            r.setFechaCreacion(guia.getFechaCreacion());
            r.setFechaActualizacion(guia.getFechaActualizacion());
            return r;
        }
    }

    @Data
    public static class HistorialResponse {
        private String instructor;
        private LocalDate fecha;
        private long totalGuias;
        private java.util.List<GuiaResponse> guias;
    }
}
