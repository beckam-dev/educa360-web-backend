package com.educa360.backend.dto;

import com.educa360.backend.entity.Matricula;
import jakarta.validation.constraints.NotNull;

/** Cambio de estado de matrícula (PENDIENTE -> PAGADO es la acción de Secretaría). */
public record MatriculaEstadoRequest(
        @NotNull Matricula.Estado estado
) {
}
