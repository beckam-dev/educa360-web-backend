package com.educa360.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Activar / desactivar una materia ({"activa": false}).
 * Desactivarla impide NUEVOS cursos; los existentes se conservan.
 */
public record MateriaActivaRequest(
        @NotNull(message = "El estado activa/inactiva es obligatorio")
        Boolean activa
) {
}
