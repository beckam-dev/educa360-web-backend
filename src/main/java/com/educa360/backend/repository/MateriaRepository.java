package com.educa360.backend.repository;

import com.educa360.backend.entity.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MateriaRepository extends JpaRepository<Materia, Long> {

    /** El nombre del catálogo es único por institución. */
    boolean existsByNombre(String nombre);
}
