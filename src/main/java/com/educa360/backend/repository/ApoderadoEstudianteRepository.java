package com.educa360.backend.repository;

import com.educa360.backend.entity.Apoderado;
import com.educa360.backend.entity.ApoderadoEstudiante;
import com.educa360.backend.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApoderadoEstudianteRepository extends JpaRepository<ApoderadoEstudiante, Long> {

    Optional<ApoderadoEstudiante> findByApoderadoIdAndEstudianteId(Long apoderadoId, Long estudianteId);

    /** Los vínculos vivos del apoderado (los hijos que puede ver). */
    List<ApoderadoEstudiante> findByApoderadoIdAndActivoTrue(Long apoderadoId);

    /** Los apoderados vivos de un estudiante (para su ficha). */
    List<ApoderadoEstudiante> findByEstudianteIdAndActivoTrue(Long estudianteId);
}
