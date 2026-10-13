package com.educa360.backend.controller;

import com.educa360.backend.dto.PeriodoEstadoRequest;
import com.educa360.backend.dto.PeriodoRequest;
import com.educa360.backend.dto.PeriodoResponse;
import com.educa360.backend.service.PeriodoAcademicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Períodos académicos (exclusivo de ADMIN).
 *
 * PATCH /{id}/estado es la única puerta para abrir un período — y sólo puede
 * haber UNO ABIERTO a la vez (si no, 409).
 */
@RestController
@RequestMapping("/api/v1/admin/periodos")
@PreAuthorize("hasRole('ADMIN')")
public class PeriodoController {

    private final PeriodoAcademicoService periodoService;

    public PeriodoController(PeriodoAcademicoService periodoService) {
        this.periodoService = periodoService;
    }

    @GetMapping
    public ResponseEntity<List<PeriodoResponse>> listar() {
        return ResponseEntity.ok(periodoService.listar());
    }

    @PostMapping
    public ResponseEntity<PeriodoResponse> crear(@Valid @RequestBody PeriodoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(periodoService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PeriodoResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody PeriodoRequest request) {
        return ResponseEntity.ok(periodoService.actualizar(id, request));
    }

    /** {"estado": "ABIERTO"} / {"estado": "CERRADO"}. */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<PeriodoResponse> cambiarEstado(
            @PathVariable Long id, @Valid @RequestBody PeriodoEstadoRequest request) {
        return ResponseEntity.ok(periodoService.cambiarEstado(id, request.estado()));
    }
}
