package com.educa360.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Reseteo de contraseña realizado por un ADMIN (el usuario no está obligado
 * a recordar la antigua).
 *
 * ATENCIÓN: al ser una API stateless, cambiar la contraseña NO invalida los
 * tokens ya emitidos; siguen siendo válidos hasta su expiración.
 */
public record UsuarioPasswordRequest(
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password
) {
}
