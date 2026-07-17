package com.duocuc.cursos.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad JPA para la tabla inscripciones_procesadas_mq.
 *
 * Almacena los datos de las cursos en línea procesadas
 * por el consumidor de la Cola 1 (cursos-cola-inscripciones).
 * Es una tabla distinta a cursos, tal como pide la sumativa.
 */
@Entity
@Table(name = "inscripciones_procesadas_mq")
public class InscripcionProcesadaMQ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String codigoCurso;

    @Column(nullable = false, length = 200)
    private String instructor;

    @Column(nullable = false, length = 200)
    private String estudiante;

    @Column(nullable = false, length = 300)
    private String tematica;

    @Column(nullable = false, length = 500)
    private String descripcion;

    private Double duracionHoras;

    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDateTime fechaProcesadaMQ;

    @Column(nullable = false, length = 50)
    private String estadoMQ; // "PROCESADO"

    @Column(length = 300)
    private String claveS3;

    public InscripcionProcesadaMQ() {}

    public InscripcionProcesadaMQ(String codigoCurso, String instructor, String estudiante,
                            String tematica, String descripcion, Double duracionHoras,
                            LocalDate fechaInicio, String claveS3) {
        this.codigoCurso       = codigoCurso;
        this.instructor    = instructor;
        this.estudiante     = estudiante;
        this.tematica = tematica;
        this.descripcion = descripcion;
        this.duracionHoras           = duracionHoras;
        this.fechaInicio    = fechaInicio;
        this.claveS3          = claveS3;
        this.fechaProcesadaMQ = LocalDateTime.now();
        this.estadoMQ         = "PROCESADO";
    }

    public Long getId()                        { return id; }
    public String getCodigoCurso()              { return codigoCurso; }
    public String getInstructor()           { return instructor; }
    public String getEstudiante()            { return estudiante; }
    public String getTematica()        { return tematica; }
    public String getDescripcion()        { return descripcion; }
    public Double getDuracionHoras()                  { return duracionHoras; }
    public LocalDate getFechaInicio()        { return fechaInicio; }
    public LocalDateTime getFechaProcesadaMQ() { return fechaProcesadaMQ; }
    public String getEstadoMQ()               { return estadoMQ; }
    public String getClaveS3()                 { return claveS3; }

    public void setId(Long id)                               { this.id = id; }
    public void setCodigoCurso(String v)                      { this.codigoCurso = v; }
    public void setInstructor(String v)                   { this.instructor = v; }
    public void setEstudiante(String v)                    { this.estudiante = v; }
    public void setTematica(String v)                { this.tematica = v; }
    public void setDescripcion(String v)                { this.descripcion = v; }
    public void setDuracionHoras(Double v)                          { this.duracionHoras = v; }
    public void setFechaInicio(LocalDate v)                { this.fechaInicio = v; }
    public void setFechaProcesadaMQ(LocalDateTime v)         { this.fechaProcesadaMQ = v; }
    public void setEstadoMQ(String v)                        { this.estadoMQ = v; }
    public void setClaveS3(String v)                         { this.claveS3 = v; }
}
