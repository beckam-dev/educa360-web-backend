package com.educa360.backend.dto;

import com.educa360.backend.entity.PeriodoAcademico;
import jakarta.validation.constraints.NotNull;

/** Abrir / cerrar un período: {"estado": "ABIERTO"|"CERRADO"}. */
public record PeriodoEstadoRequest(
        @NotNull(message = "El estado (ABIERTO/CERRADO) es obligatorio")
        PeriodoAcademico.Estado estado
) {
}
