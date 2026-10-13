package com.educa360.backend.repository;

import com.educa360.backend.entity.AsignacionDocente;
import com.educa360.backend.entity.Docente;
import com.educa360.backend.entity.Materia;
import com.educa360.backend.entity.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AsignacionDocenteRepository extends JpaRepository<AsignacionDocente, Long> {

    /** Una materia sólo puede tener UN docente por sección (nadie más da Matemáticas en 3°A). */
    boolean existsByMateriaAndSeccion(Materia materia, Seccion seccion);

    List<AsignacionDocente> findBySeccionId(Long seccionId);

    List<AsignacionDocente> findByDocenteId(Long docenteId);
}
