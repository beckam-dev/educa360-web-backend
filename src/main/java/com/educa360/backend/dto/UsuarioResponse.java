package com.educa360.backend.dto;

import com.educa360.backend.entity.Telefono;
import com.educa360.backend.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vista de un usuario para administración: datos básicos + su perfil.
 *
 * Los campos de perfil (titulo, fechaNacimiento, estadoEstudiante) son
 * null cuando no corresponden al rol; la contraseña NUNCA sale.
 */
public record UsuarioResponse(
        Long id,
        String email,
        String nombres,
        String apellidos,
        String dni,
        User.Role rol,
        boolean activo,
        List<String> telefonos,
        String titulo,
        LocalDate fechaNacimiento,
        String estadoEstudiante,
        LocalDateTime createdAt
) {
    public static UsuarioResponse desde(User user, String titulo,
                                        LocalDate fechaNacimiento, String estadoEstudiante) {
        return new UsuarioResponse(
                user.getId(),
                user.getEmail(),
                user.getNombres(),
                user.getApellidos(),
                user.getDni(),
                user.getRol(),
                user.isActivo(),
                user.getTelefonos().stream().map(Telefono::getNumero).sorted().toList(),
                titulo,
                fechaNacimiento,
                estadoEstudiante,
                user.getCreatedAt()
        );
    }
}
