package com.educa360.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Hoja completa del horario de un curso ({"bloques": [...]}).
 * El PUT pinta la hoja entera — misma filosofía que las habilitaciones:
 * el resultado no depende del estado previo.
 */
public record HorarioRequest(
        @NotNull(message = "La lista de bloques es obligatoria (puede ser vacía)")
        List<@Valid @NotNull HorarioBloqueRequest> bloques
) {
}
