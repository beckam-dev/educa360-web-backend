package com.educa360.backend.dto;

import com.educa360.backend.entity.AsignacionDocente;

import java.time.LocalDateTime;

/**
 * Ficha de una asignación ("el curso" del colegio): en el panel se ve como
 * Matemática · 3°A · docente · período.
 */
public record AsignacionResponse(
        Long id,
        Long docenteId,
        String docenteNombre,
        Long materiaId,
        String materiaNombre,
        Long seccionId,
        String seccionEtiqueta, // "3°A"
        Long periodoId,
        String periodoNombre,
        LocalDateTime createdAt
) {
    public static AsignacionResponse desde(AsignacionDocente asignacion) {
        return new AsignacionResponse(
                asignacion.getId(),
                asignacion.getDocente().getId(),
                asignacion.getDocente().getUser().getNombres() + " "
                        + asignacion.getDocente().getUser().getApellidos(),
                asignacion.getMateria().getId(),
                asignacion.getMateria().getNombre(),
                asignacion.getSeccion().getId(),
                asignacion.getSeccion().getEtiqueta(),
                asignacion.getSeccion().getPeriodo().getId(),
                asignacion.getSeccion().getPeriodo().getNombre(),
                asignacion.getCreatedAt()
        );
    }
}
