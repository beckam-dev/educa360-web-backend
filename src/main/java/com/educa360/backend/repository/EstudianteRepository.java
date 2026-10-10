package com.educa360.backend.repository;

import com.educa360.backend.entity.Estudiante;
import com.educa360.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    Optional<Estudiante> findByUser(User user);
}
