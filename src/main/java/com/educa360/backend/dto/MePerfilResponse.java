package com.educa360.backend.dto;

import java.util.List;

/**
 * Perfil del rol dentro de /auth/me.
 *
 * Hoy sólo el DOCENTE trae datos (título y materias habilitadas). El
 * ESTUDIANTE añadirá su sección y el APODERADO sus hijos con el Módulo 3
 * (matrículas) — de ahí que los campos sean opcionales y los demás roles
 * lleven perfil null.
 */
public record MePerfilResponse(String titulo, List<MateriaResponse> materias) {
}
