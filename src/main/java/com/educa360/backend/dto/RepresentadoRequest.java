package com.educa360.backend.dto;

import com.educa360.backend.entity.ApoderadoEstudiante;
import jakarta.validation.constraints.NotNull;

/**
 * Vínculo apoderado ↔ estudiante que gestiona Secretaría (§5, acceso por
 * perfil: el apoderado sólo CONSULTA lo que Secretaría le ha asignado).
 */
public record RepresentadoRequest(
        @NotNull Long estudianteId,
        @NotNull ApoderadoEstudiante.Parentesco parentesco,
        @NotNull Boolean responsablePrincipal
) {
}
