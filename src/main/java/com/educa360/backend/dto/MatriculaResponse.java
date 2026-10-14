package com.educa360.backend.dto;

import com.educa360.backend.entity.Matricula;

import java.time.LocalDateTime;

/** Matrícula con su sección y período — la ficha que Secretaría confirma tras inscribir. */
public record MatriculaResponse(
        Long id,
        Long estudianteId,
        String estudianteNombre,
        Long seccionId,
        String seccionEtiqueta, // "3°A"
        Long periodoId,
        String periodoNombre,
        Matricula.Estado estado,
        LocalDateTime createdAt
) {
    public static MatriculaResponse desde(Matricula matricula) {
        return new MatriculaResponse(
                matricula.getId(),
                matricula.getEstudiante().getId(),
                matricula.getEstudiante().getUser().getNombres() + " "
                        + matricula.getEstudiante().getUser().getApellidos(),
                matricula.getSeccion().getId(),
                matricula.getSeccion().getEtiqueta(),
                matricula.getSeccion().getPeriodo().getId(),
                matricula.getSeccion().getPeriodo().getNombre(),
                matricula.getEstado(),
                matricula.getCreatedAt()
        );
    }
}
