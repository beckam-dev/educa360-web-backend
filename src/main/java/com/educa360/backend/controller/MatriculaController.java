package com.educa360.backend.controller;

import com.educa360.backend.dto.MatriculaEstadoRequest;
import com.educa360.backend.dto.MatriculaRequest;
import com.educa360.backend.dto.MatriculaResponse;
import com.educa360.backend.service.MatriculaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Matrículas — dominio de SECRETARIA con ADMIN como superconjunto (§3).
 *
 * Nacen PENDIENTEs; el paso a PAGADO es una acción explícita de Secretaría
 * (PATCH /{id}/estado), nunca automática.
 */
@RestController
@RequestMapping("/api/v1/secretaria/matriculas")
@PreAuthorize("hasAnyRole('ADMIN', 'SECRETARIA')")
public class MatriculaController {

    private final MatriculaService matriculaService;

    public MatriculaController(MatriculaService matriculaService) {
        this.matriculaService = matriculaService;
    }

    /** Inscribe al alumno en la sección: {"estudianteId":1,"seccionId":2} */
    @PostMapping
    public ResponseEntity<MatriculaResponse> crear(
            @Valid @RequestBody MatriculaRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matriculaService.crear(datos));
    }

    /** Pasa la matrícula a PAGADO (o la devuelve a PENDIENTE): {"estado":"PAGADO"} */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<MatriculaResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody MatriculaEstadoRequest datos) {
        return ResponseEntity.ok(matriculaService.cambiarEstado(id, datos));
    }
}
