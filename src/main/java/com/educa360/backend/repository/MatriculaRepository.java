package com.educa360.backend.repository;

import com.educa360.backend.entity.Estudiante;
import com.educa360.backend.entity.Matricula;
import com.educa360.backend.entity.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    boolean existsByEstudianteAndSeccion(Estudiante estudiante, Seccion seccion);

    /** ¿Ya tiene el estudiante una matrícula en ESTE período? (en otra sección) */
    boolean existsByEstudianteIdAndSeccionPeriodoId(Long estudianteId, Long periodoId);

    /** La matrícula vigente del alumno: la del período ABIERTO. */
    Optional<Matricula> findByEstudianteIdAndSeccionPeriodoId(Long estudianteId, Long periodoId);

    /** Todas las matrículas de un período (para el listado de Secretaría). */
    List<Matricula> findBySeccionPeriodoId(Long periodoId);
}
