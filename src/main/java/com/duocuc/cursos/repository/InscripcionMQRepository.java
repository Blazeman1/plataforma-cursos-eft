package com.duocuc.cursos.repository;

import com.duocuc.cursos.model.InscripcionProcesadaMQ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InscripcionMQRepository extends JpaRepository<InscripcionProcesadaMQ, Long> {
}
