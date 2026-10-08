package com.educa360.backend.dto;

import com.educa360.backend.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * Alta de cuenta de usuario.
 *
 * IMPORTANTE: crea únicamente la cuenta de acceso (User).
 * Los perfiles (Docente, Estudiante, Apoderado, Secretaria) los crea
 * el módulo de gestión de usuarios, que aún no existe.
 *
 * El rol es opcional: si no viene, se asume ADMIN (sólo tiene efecto
 * en el alta inicial del primer usuario, ver AuthService#register).
 */
public record RegisterRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank(message = "Los nombres son obligatorios")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        String apellidos,

        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos")
        String dni,

        User.Role rol
) {
    // Normalización al nacer: el email no distingue mayúsculas y el DNI no lleva espacios
    public RegisterRequest {
        email = (email == null) ? null : email.trim().toLowerCase(Locale.ROOT);
        dni = (dni == null) ? null : dni.trim();
    }
}
