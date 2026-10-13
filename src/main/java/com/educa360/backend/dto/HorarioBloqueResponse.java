package com.educa360.backend.dto;

import com.educa360.backend.entity.DiaSemana;
import com.educa360.backend.entity.HorarioBloque;

import java.time.LocalTime;

/** Un bloque del horario, para el panel del curso o de la sección. */
public record HorarioBloqueResponse(
        Long id,
        DiaSemana dia,
        LocalTime horaInicio,
        LocalTime horaFin
) {
    public static HorarioBloqueResponse desde(HorarioBloque bloque) {
        return new HorarioBloqueResponse(
                bloque.getId(),
                bloque.getDia(),
                bloque.getHoraInicio(),
                bloque.getHoraFin()
        );
    }
}
