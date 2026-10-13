package com.educa360.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Alta de una asignación docente: qué docente enseña qué materia en qué sección.
 * El período NO se pide: viene dado por la sección.
 */
public record AsignacionRequest(
        @NotNull(message = "El docente es obligatorio")
        Long docenteId,

        @NotNull(message = "La materia es obligatoria")
        Long materiaId,

        @NotNull(message = "La sección es obligatoria")
        Long seccionId
) {
}
