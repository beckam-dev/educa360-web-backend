package com.educa360.backend.dto;

import com.educa360.backend.entity.DiaSemana;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Un bloque del horario semanal: {"dia":"LUNES","horaInicio":"08:00","horaFin":"09:00"}.
 * Se repite cada semana: es la sesión a la que colgará la asistencia.
 */
public record HorarioBloqueRequest(
        @NotNull(message = "El día (LUNES..SABADO) es obligatorio")
        DiaSemana dia,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin
) {
}
