package com.educa360.backend.dto;

import com.educa360.backend.entity.PeriodoAcademico;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Ficha de un período para el panel de ADMIN. */
public record PeriodoResponse(
        Long id,
        String nombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        PeriodoAcademico.Estado estado,
        LocalDateTime createdAt
) {
    public static PeriodoResponse desde(PeriodoAcademico periodo) {
        return new PeriodoResponse(
                periodo.getId(),
                periodo.getNombre(),
                periodo.getFechaInicio(),
                periodo.getFechaFin(),
                periodo.getEstado(),
                periodo.getCreatedAt()
        );
    }
}
