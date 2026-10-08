package com.educa360.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales de acceso. El email es el "username" del sistema.
 */
public record LoginRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
