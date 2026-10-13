package com.educa360.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Alta de un curso: qué docente enseña qué materia en qué sección.
 * El período NO se pide: viene dado por la sección. El horario se configura
 * después, con PUT /admin/cursos/{id}/horario.
 */
public record CursoRequest(
        @NotNull(message = "El docente es obligatorio")
        Long docenteId,

        @NotNull(message = "La materia es obligatoria")
        Long materiaId,

        @NotNull(message = "La sección es obligatoria")
        Long seccionId
) {
}
