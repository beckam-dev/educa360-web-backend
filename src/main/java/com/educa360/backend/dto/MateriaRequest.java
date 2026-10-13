package com.educa360.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Alta/edición de una materia del catálogo institucional ("lo que se enseña"). */
public record MateriaRequest(
        @NotBlank(message = "El nombre de la materia es obligatorio")
        String nombre,

        String descripcion
) {
}
