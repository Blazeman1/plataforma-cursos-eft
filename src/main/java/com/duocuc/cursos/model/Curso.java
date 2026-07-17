package com.duocuc.cursos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cursos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigoCurso;

    @Column(nullable = false)
    private String instructor;

    @Column(nullable = false)
    private String estudiante;

    @Column(nullable = false)
    private String tematica;

    @Column(nullable = false)
    private String descripcion;

    private Double duracionHoras;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Enumerated(EnumType.STRING)
    private EstadoCurso estado;

    // Rutas de almacenamiento
    private String rutaEfs;      // Ruta temporal en EFS
    private String claveS3;      // Clave del objeto en S3

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (estado == null) estado = EstadoCurso.PENDIENTE;
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    public enum EstadoCurso {
        PENDIENTE, GENERADA, SUBIDA_S3, ENTREGADA, CANCELADA
    }
}
