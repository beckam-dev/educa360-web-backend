package com.educa360.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Alta/edición de un período académico. Nace CERRADO; se abre con PATCH /estado. */
public record PeriodoRequest(
        @NotBlank(message = "El nombre del período es obligatorio")
        String nombre,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin
) {
}
