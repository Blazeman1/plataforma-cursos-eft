package com.duocuc.cursos.repository;

import com.duocuc.cursos.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {

    Optional<Curso> findByNumeroGuia(String codigoCurso);

    List<Curso> findByTransportista(String instructor);

    List<Curso> findByFechaDespacho(LocalDate fecha);

    List<Curso> findByTransportistaAndFechaDespacho(String instructor, LocalDate fecha);

    boolean existsByNumeroGuia(String codigoCurso);
}
