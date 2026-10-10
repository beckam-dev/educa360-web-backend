package com.educa360.backend.dto;

import jakarta.validation.constraints.NotNull;

/** Activar / desactivar la cuenta (baja lógica: corta el acceso al instante). */
public record UsuarioEstadoRequest(
        @NotNull(message = "El estado activo/inactivo es obligatorio")
        Boolean activo
) {
}
