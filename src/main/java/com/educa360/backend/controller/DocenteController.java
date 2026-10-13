package com.educa360.backend.controller;

import com.educa360.backend.dto.DocenteMateriasRequest;
import com.educa360.backend.dto.DocenteRequest;
import com.educa360.backend.dto.MateriaResponse;
import com.educa360.backend.dto.UsuarioBasicoRequest;
import com.educa360.backend.dto.UsuarioResponse;
import com.educa360.backend.service.DocenteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Altas de DOCENTE y sus habilitaciones de materia (exclusivo de ADMIN).
 *
 * Cuerpo del alta: {"usuario": {...}, "perfil": {...}} — la cuenta y sus datos
 * propios separados, de modo que el perfil puede reutilizarse en otros
 * módulos sin arrastrar credenciales.
 */
@RestController
@RequestMapping("/api/v1/admin/docentes")
@PreAuthorize("hasRole('ADMIN')")
public class DocenteController {

    private final DocenteService docenteService;

    public DocenteController(DocenteService docenteService) {
        this.docenteService = docenteService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(
            @Valid @RequestBody Cuerpo body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(docenteService.crear(body.usuario(), body.perfil()));
    }

    // Habilitaciones de materia: "qué puede enseñar" (el alta de cursos lo exige)

    @GetMapping("/{id}/materias")
    public ResponseEntity<List<MateriaResponse>> listarMaterias(@PathVariable Long id) {
        return ResponseEntity.ok(docenteService.listarMaterias(id));
    }

    /** Reemplaza el conjunto completo: envía todas las materias habilitadas (o lista vacía). */
    @PutMapping("/{id}/materias")
    public ResponseEntity<List<MateriaResponse>> habilitarMaterias(
            @PathVariable Long id, @Valid @RequestBody DocenteMateriasRequest request) {
        return ResponseEntity.ok(docenteService.habilitarMaterias(id, request));
    }

    /** Envoltorio del request: parte común (usuario) + parte del rol (perfil). */
    public record Cuerpo(
            @Valid UsuarioBasicoRequest usuario,
            @Valid @NotNull DocenteRequest perfil) {
    }
}
