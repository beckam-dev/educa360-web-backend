package com.educa360.backend.controller;

import com.educa360.backend.dto.CursoRequest;
import com.educa360.backend.dto.CursoResponse;
import com.educa360.backend.dto.HorarioBloqueResponse;
import com.educa360.backend.dto.HorarioRequest;
import com.educa360.backend.service.CursoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cursos — "el curso" del colegio: Matemática · 3°A · profe X.
 * Exclusivo de ADMIN.
 *
 * El período no se pide: viene de la sección. El docente debe tener la materia
 * habilitada antes (ver GET/PUT /admin/docentes/{id}/materias).
 * El horario se pinta por hoja entera en /{id}/horario: cada bloque es una
 * sesión semanal (a ella colgará la asistencia).
 */
@RestController
@RequestMapping("/api/v1/admin/cursos")
@PreAuthorize("hasRole('ADMIN')")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    /** Filtrable con ?seccionId= o ?docenteId=. */
    @GetMapping
    public ResponseEntity<List<CursoResponse>> listar(
            @RequestParam(required = false) Long seccionId,
            @RequestParam(required = false) Long docenteId) {
        return ResponseEntity.ok(cursoService.listar(seccionId, docenteId));
    }

    @PostMapping
    public ResponseEntity<CursoResponse> crear(@Valid @RequestBody CursoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cursoService.crear(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cursoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // Horario del curso

    @GetMapping("/{id}/horario")
    public ResponseEntity<List<HorarioBloqueResponse>> listarHorario(@PathVariable Long id) {
        return ResponseEntity.ok(cursoService.listarHorario(id));
    }

    /** Reemplaza la hoja entera: {"bloques": [{"dia":"LUNES","horaInicio":"08:00","horaFin":"09:00"}, ...]}. */
    @PutMapping("/{id}/horario")
    public ResponseEntity<List<HorarioBloqueResponse>> reemplazarHorario(
            @PathVariable Long id, @Valid @RequestBody HorarioRequest request) {
        return ResponseEntity.ok(cursoService.reemplazarHorario(id, request));
    }
}
