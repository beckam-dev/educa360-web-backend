package com.educa360.backend.controller;

import com.educa360.backend.dto.DocenteRequest;
import com.educa360.backend.dto.UsuarioBasicoRequest;
import com.educa360.backend.dto.UsuarioResponse;
import com.educa360.backend.service.DocenteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Altas de DOCENTE (exclusivo de ADMIN).
 *
 * Cuerpo: {"usuario": {...}, "perfil": {...}} — la cuenta y sus datos
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

    /** Envoltorio del request: parte común (usuario) + parte del rol (perfil). */
    public record Cuerpo(
            @Valid UsuarioBasicoRequest usuario,
            @Valid @NotNull DocenteRequest perfil) {
    }
}
