package com.educa360.backend.repository;

import com.educa360.backend.entity.PeriodoAcademico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PeriodoAcademicoRepository extends JpaRepository<PeriodoAcademico, Long> {

    boolean existsByNombre(String nombre);

    /** ¿Hay otro período abierto además de 'id'? (regla: sólo uno vigente) */
    boolean existsByEstadoAndIdNot(PeriodoAcademico.Estado estado, Long id);

    /** El período vigente: sólo puede haber uno ABIERTO, por eso Optional. */
    Optional<PeriodoAcademico> findByEstado(PeriodoAcademico.Estado estado);
}
