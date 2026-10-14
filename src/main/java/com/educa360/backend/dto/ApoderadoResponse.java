package com.educa360.backend.dto;

import com.educa360.backend.entity.ApoderadoEstudiante;

import java.util.List;

/** Apoderado con sus representados vivos — el listado de familias de Secretaría. */
public record ApoderadoResponse(
        Long id, // id del perfil
        String email,
        String nombres,
        String apellidos,
        String dni,
        List<String> telefonos,
        boolean activo,
        List<HijoBasico> hijos
) {
    /** Lo mínimo para pintar la lista: quién es y qué parentesco tiene. */
    public record HijoBasico(
            Long estudianteId,
            String nombres,
            ApoderadoEstudiante.Parentesco parentesco,
            boolean responsablePrincipal
    ) {
    }
}
