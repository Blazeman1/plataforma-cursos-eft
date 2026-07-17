package com.duocuc.cursos.dto;

import com.duocuc.cursos.model.Curso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CursoDTO {

    @Data
    public static class CrearCursoRequest {
        @NotBlank(message = "El instructor es obligatorio")
        private String instructor;

        @NotBlank(message = "El estudiante es obligatorio")
        private String estudiante;

        @NotBlank(message = "La temática es obligatoria")
        private String tematica;

        @NotBlank(message = "La descripción es obligatoria")
        private String descripcion;

        private Double duracionHoras;

        @NotNull(message = "La fecha de inicio es obligatoria")
        private LocalDate fechaInicio;
    }

    @Data
    public static class ActualizarCursoRequest {
        private String estudiante;
        private String tematica;
        private String descripcion;
        private Double duracionHoras;
        private LocalDate fechaInicio;
        private Curso.EstadoCurso estado;
    }

    @Data
    public static class CursoResponse {
        private Long id;
        private String codigoCurso;
        private String instructor;
        private String estudiante;
        private String tematica;
        private String descripcion;
        private Double duracionHoras;
        private LocalDate fechaInicio;
        private Curso.EstadoCurso estado;
        private String rutaEfs;
        private String claveS3;
        private LocalDateTime fechaCreacion;
        private LocalDateTime fechaActualizacion;

        public static CursoResponse from(Curso curso) {
            CursoResponse r = new CursoResponse();
            r.setId(curso.getId());
            r.setCodigoCurso(curso.getCodigoCurso());
            r.setInstructor(curso.getInstructor());
            r.setEstudiante(curso.getEstudiante());
            r.setTematica(curso.getTematica());
            r.setDescripcion(curso.getDescripcion());
            r.setDuracionHoras(curso.getDuracionHoras());
            r.setFechaInicio(curso.getFechaInicio());
            r.setEstado(curso.getEstado());
            r.setRutaEfs(curso.getRutaEfs());
            r.setClaveS3(curso.getClaveS3());
            r.setFechaCreacion(curso.getFechaCreacion());
            r.setFechaActualizacion(curso.getFechaActualizacion());
            return r;
        }
    }

    @Data
    public static class HistorialResponse {
        private String instructor;
        private LocalDate fecha;
        private long totalCursos;
        private java.util.List<CursoResponse> cursos;
    }
}
