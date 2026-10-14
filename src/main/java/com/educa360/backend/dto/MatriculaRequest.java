package com.educa360.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Alta de matrícula desde Secretaría: "inscribe a este alumno en esta sección".
 *
 * El estado no viaja aquí: la matrícula nace PENDIENTE por regla y SÓLO
 * SECRETARIA la pasa a PAGADO con PATCH /{id}/estado (§3).
 */
public record MatriculaRequest(
        @NotNull Long estudianteId,
        @NotNull Long seccionId
) {
}
