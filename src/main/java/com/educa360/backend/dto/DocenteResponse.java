package com.educa360.backend.dto;

import com.educa360.backend.entity.Docente;

import java.util.List;

/**
 * Ficha del docente para el panel de ADMIN (gestión y habilitaciones).
 *
 * Trae el id del PERFIL (no el de la cuenta): las habilitaciones cuelgan de
 * /admin/docentes/{id}, y la lista de cuentas ya la da GET /admin/usuarios.
 * Meter ids de perfil en UsuarioResponse rompería su decisión (vista de
 * cuentas única) escrita en su javadoc: cada pantalla, su salida.
 */
public record DocenteResponse(
        Long id, // id del perfil Docente
        String email,
        String nombres,
        String apellidos,
        String dni,
        String titulo,
        List<MateriaResponse> materias, // habilitadas hoy
        boolean activo
) {
    public static DocenteResponse desde(Docente docente) {
        return new DocenteResponse(
                docente.getId(),
                docente.getUser().getEmail(),
                docente.getUser().getNombres(),
                docente.getUser().getApellidos(),
                docente.getUser().getDni(),
                docente.getTitulo(),
                docente.getMaterias().stream().map(MateriaResponse::desde).toList(),
                docente.getUser().isActivo()
        );
    }
}
