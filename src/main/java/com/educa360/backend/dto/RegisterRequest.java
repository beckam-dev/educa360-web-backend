package com.educa360.backend.dto;

import com.educa360.backend.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta de cuenta de usuario.
 *
 * IMPORTANTE: crea únicamente la cuenta de acceso (User).
 * Los perfiles (Docente, Estudiante, Apoderado, Secretaria) los crea
 * el módulo de gestión de usuarios, que aún no existe.
 *
 * El rol es opcional: si no viene, se asume ADMIN (sólo tiene efecto
 * en el bootstrap del primer usuario, ver AuthService#register).
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
        String dni,

        User.Role rol
) {
}
