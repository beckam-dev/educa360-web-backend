package com.educa360.backend.controller;

import com.educa360.backend.dto.AsignacionRequest;
import com.educa360.backend.dto.AsignacionResponse;
import com.educa360.backend.service.AsignacionDocenteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Asignaciones docentes — "el curso" del colegio: Matemática · 3°A · profe X.
 * Exclusivo de ADMIN.
 *
 * El período no se pide: viene de la sección. El docente debe tener la materia
 * habilitada antes (ver GET/PUT /admin/docentes/{id}/materias).
 */
@RestController
@RequestMapping("/api/v1/admin/asignaciones")
@PreAuthorize("hasRole('ADMIN')")
public class AsignacionDocenteController {

    private final AsignacionDocenteService asignacionService;

    public AsignacionDocenteController(AsignacionDocenteService asignacionService) {
        this.asignacionService = asignacionService;
    }

    /** Filtrable con ?seccionId= o ?docenteId=. */
    @GetMapping
    public ResponseEntity<List<AsignacionResponse>> listar(
            @RequestParam(required = false) Long seccionId,
            @RequestParam(required = false) Long docenteId) {
        return ResponseEntity.ok(asignacionService.listar(seccionId, docenteId));
    }

    @PostMapping
    public ResponseEntity<AsignacionResponse> crear(@Valid @RequestBody AsignacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asignacionService.crear(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        asignacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
