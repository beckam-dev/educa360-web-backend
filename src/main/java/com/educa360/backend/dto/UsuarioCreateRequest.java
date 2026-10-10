package com.educa360.backend.dto;

import com.educa360.backend.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Alta de usuario desde el módulo de administración.
 *
 * La regla de consistencia perfil ↔ rol vive aquí: el rol del request decide
 * qué perfil se crea (y con qué datos obligatorios). Así es imposible que
 * exista un User ESTUDIANTE con perfil DOCENTE.
 *
 * - ESTUDIANTE: exige fechaNacimiento y NO admite teléfonos (comunicación
 *   vía apoderado, al ser menores de edad).
 * - DOCENTE: puede llevar titulo.
 * - Los demás roles: sólo los datos básicos + teléfonos opcionales.
 */
public record UsuarioCreateRequest(
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

        @NotNull(message = "El rol es obligatorio")
        User.Role rol,

        List<@NotBlank(message = "El teléfono no puede estar vacío")
        @Pattern(regexp = "\\+?\\d{7,15}", message = "Teléfono inválido: sólo dígitos, opcionalmente con +") String> telefonos,

        LocalDate fechaNacimiento,

        String titulo
) {
    public UsuarioCreateRequest {
        email = (email == null) ? null : email.trim().toLowerCase(Locale.ROOT);
        dni = (dni == null) ? null : dni.trim();
        if (telefonos != null) {
            telefonos = telefonos.stream().map(t -> (t == null) ? null : t.trim()).toList();
        }
    }
}
