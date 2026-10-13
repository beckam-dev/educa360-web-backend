package com.educa360.backend.repository;

import com.educa360.backend.entity.Docente;
import com.educa360.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Long> {

    Optional<Docente> findByUser(User user);

    /** ¿El docente tiene habilitada esa materia? (join table docente_materia) */
    boolean existsByIdAndMaterias_Id(Long docenteId, Long materiaId);
}
