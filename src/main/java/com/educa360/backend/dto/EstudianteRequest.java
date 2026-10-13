package com.educa360.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Datos propios del perfil ESTUDIANTE.
 *
 * Se reutiliza fuera del alta: el módulo de SECRETARIA lo volverá a usar para
 * completar o editar la ficha del alumno (sección, apoderado...).
 */
public record EstudianteRequest(
        @NotNull(message = "El estudiante debe registrar su fecha de nacimiento")
        LocalDate fechaNacimiento
) {
}
