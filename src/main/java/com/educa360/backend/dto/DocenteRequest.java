package com.educa360.backend.dto;

/**
 * Datos propios del perfil DOCENTE.
 *
 * El título es opcional hoy; más adelante aquí entrarán sus materias
 * (asignación del catálogo académico).
 */
public record DocenteRequest(String titulo) {
}
