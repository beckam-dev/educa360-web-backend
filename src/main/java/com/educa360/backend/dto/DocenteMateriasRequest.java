package com.educa360.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Habilitaciones de un docente ({@code PUT /admin/docentes/{id}/materias}).
 * Envía la lista COMPLETA de materias que el docente puede enseñar:
 * reemplaza el conjunto (el endpoint pinta la hoja entera, no suma/resta filas).
 */
public record DocenteMateriasRequest(
        @NotNull(message = "La lista de materias es obligatoria (puede ser vacía)")
        List<@NotNull(message = "Cada id de materia debe ser un número") Long> materiaIds
) {
}
