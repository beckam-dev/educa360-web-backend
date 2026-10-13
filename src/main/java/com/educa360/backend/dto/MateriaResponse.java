package com.educa360.backend.dto;

import com.educa360.backend.entity.Materia;

/** Ficha de una materia del catálogo. */
public record MateriaResponse(
        Long id,
        String nombre,
        String descripcion,
        boolean activa
) {
    public static MateriaResponse desde(Materia materia) {
        return new MateriaResponse(
                materia.getId(),
                materia.getNombre(),
                materia.getDescripcion(),
                materia.isActiva()
        );
    }
}
