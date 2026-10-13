package com.educa360.backend.repository;

import com.educa360.backend.entity.Curso;
import com.educa360.backend.entity.DiaSemana;
import com.educa360.backend.entity.HorarioBloque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HorarioBloqueRepository extends JpaRepository<HorarioBloque, Long> {

    List<HorarioBloque> findByCursoId(Long cursoId);

    /** Bloques del mismo día de TODOS los cursos de una sección (para cazar solapes). */
    List<HorarioBloque> findByCursoSeccionIdAndDia(Long seccionId, DiaSemana dia);

    /** Bloques del mismo día de TODOS los cursos de un docente (idem). */
    List<HorarioBloque> findByCursoDocenteIdAndDia(Long docenteId, DiaSemana dia);

    /** Baja en bloque para reemplazar la hoja o eliminar el curso. */
    void deleteByCurso(Curso curso);
}
