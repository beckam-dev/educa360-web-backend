package com.educa360.backend.controller;

import com.educa360.backend.dto.CursoResponse;
import com.educa360.backend.service.CursoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Zona del ESTUDIANTE: los cursos de SU sección (la del período vigente).
 *
 * Mismo criterio que /docente/cursos: sin filtros, la identidad sale del
 * token. Sin matrícula (o sin período abierto) devuelve lista vacía —
 * no es un error, es un alumno aún no inscrito.
 */
@RestController
@RequestMapping("/api/v1/estudiante/cursos")
@PreAuthorize("hasRole('ESTUDIANTE')")
public class EstudianteCursoController {

    private final CursoService cursoService;

    public EstudianteCursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    public ResponseEntity<List<CursoResponse>> misCursos(Authentication authentication) {
        return ResponseEntity.ok(cursoService.listarDelEstudiante(authentication.getName()));
    }
}
