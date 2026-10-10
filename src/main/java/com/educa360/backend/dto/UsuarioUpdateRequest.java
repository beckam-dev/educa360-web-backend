package com.educa360.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

/**
 * Edición de un usuario existente.
 *
 * No se puede cambiar el rol: eso implicaría destruir y recrear el perfil
 * (p. ej. DOCENTE -> ESTUDIANTE), y el sistema prevé dar de baja y crear
 * una cuenta nueva en su lugar.
 *
 * fechaNacimiento y titulo se ignoran si no corresponden al rol del usuario.
 */
public record UsuarioUpdateRequest(
        @NotBlank(message = "Los nombres son obligatorios")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        String apellidos,

        // null = no tocar los teléfonos; lista vacía = quitar todos
        List<@NotBlank(message = "El teléfono no puede estar vacío")
        @Pattern(regexp = "\\+?\\d{7,15}", message = "Teléfono inválido: sólo dígitos, opcionalmente con +") String> telefonos,

        LocalDate fechaNacimiento,

        String titulo
) {
}
