package com.educa360.backend.dto;

import com.educa360.backend.entity.Telefono;
import com.educa360.backend.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vista de una cuenta para el panel de administración: datos básicos + los
 * campos de su perfil. La contraseña NUNCA sale.
 *
 * DECISIÓN DE DISEÑO: un solo DTO con los campos de perfil anulables
 * (titulo sólo en DOCENTE, fechaNacimiento/estadoEstudiante sólo en
 * ESTUDIANTE), en vez de un DTO por rol. La regla del proyecto es:
 * las ENTRADAS se dividen por ROL (porque las reglas de validación difieren:
 * un estudiante exige fecha de nacimiento y prohíbe teléfonos), mientras que
 * las SALIDAS se dividen por PANTALLA (porque la tabla de ADMIN lista todos
 * los roles en las mismas columnas; partirlo por rol obligaría a mezclar
 * tres tipos en una sola tabla).
 *
 * Se partirá cuando exista un segundo consumidor con otras necesidades:
 * p. ej. cuando Secretaria liste alumnos necesitará sección, matrícula y
 * apoderado, y ahí nacerá un EstudianteResponse propio.
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
