package com.educa360.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Locale;

/**
 * Datos comunes de la CUENTA de acceso: credenciales e identidad básica.
 *
 * No es un request autónomo: es la parte que comparten los endpoints de alta
 * de cada rol ({@code POST /admin/docentes}, {@code /secretaria/estudiantes}...),
 * donde el cuerpo es {@code {"usuario": {...}, "perfil": {...}}}.
 *
 * NO lleva el rol: el rol lo decide el endpoint (DocenteService crea un
 * DOCENTE, EstudianteService un ESTUDIANTE...), así que un request no puede
 * "mentir" sobre el rol que va a tener la cuenta.
 */
public record UsuarioBasicoRequest(
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

        // Null o vacío = sin teléfonos. El ESTUDIANTE no debe traer ninguno
        // (lo hace cumplir EstudianteService: la comunicación es con su apoderado).
        List<@NotBlank(message = "El teléfono no puede estar vacío")
        @Pattern(regexp = "\\+?\\d{7,15}", message = "Teléfono inválido: sólo dígitos, opcionalmente con +") String> telefonos
) {
    public UsuarioBasicoRequest {
        email = (email == null) ? null : email.trim().toLowerCase(Locale.ROOT);
        dni = (dni == null) ? null : dni.trim();
        if (telefonos != null) {
            telefonos = telefonos.stream().map(t -> (t == null) ? null : t.trim()).toList();
        }
    }
}
