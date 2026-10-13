package com.educa360.backend.controller;

import com.educa360.backend.dto.MateriaActivaRequest;
import com.educa360.backend.dto.MateriaRequest;
import com.educa360.backend.dto.MateriaResponse;
import com.educa360.backend.service.MateriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catálogo de materias (exclusivo de ADMIN).
 *
 * Materia = "lo que se enseña" (Matemática, Física...) — sin sección ni docente.
 * PATCH /{id}/activa la archiva sin borrar: impide nuevas asignaciones,
 * las existentes se conservan.
 */
@RestController
@RequestMapping("/api/v1/admin/materias")
@PreAuthorize("hasRole('ADMIN')")
public class MateriaController {

    private final MateriaService materiaService;

    public MateriaController(MateriaService materiaService) {
        this.materiaService = materiaService;
    }

    @GetMapping
    public ResponseEntity<List<MateriaResponse>> listar() {
        return ResponseEntity.ok(materiaService.listar());
    }

    @PostMapping
    public ResponseEntity<MateriaResponse> crear(@Valid @RequestBody MateriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materiaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MateriaResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody MateriaRequest request) {
        return ResponseEntity.ok(materiaService.actualizar(id, request));
    }

    /** {"activa": true|false} — el archivado que consulta el alta de asignaciones. */
    @PatchMapping("/{id}/activa")
    public ResponseEntity<MateriaResponse> cambiarActiva(
            @PathVariable Long id, @Valid @RequestBody MateriaActivaRequest request) {
        return ResponseEntity.ok(materiaService.cambiarActiva(id, request.activa()));
    }
}
