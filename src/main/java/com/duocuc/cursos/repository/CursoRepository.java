package com.duocuc.cursos.repository;

import com.duocuc.cursos.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {

    Optional<Curso> findByCodigoCurso(String codigoCurso);

    List<Curso> findByInstructor(String instructor);

    List<Curso> findByFechaInicio(LocalDate fecha);

    List<Curso> findByInstructorAndFechaInicio(String instructor, LocalDate fecha);

    boolean existsByCodigoCurso(String codigoCurso);
}
