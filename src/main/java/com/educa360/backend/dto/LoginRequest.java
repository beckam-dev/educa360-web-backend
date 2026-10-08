package com.educa360.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

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
    // Normalización al nacer: "  Juan@X.com " y "juan@x.com" son la MISMA cuenta.
    // Sin esto, Postgres (case-sensitive) crearía dos usuarios distintos.
    public LoginRequest {
        email = (email == null) ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
