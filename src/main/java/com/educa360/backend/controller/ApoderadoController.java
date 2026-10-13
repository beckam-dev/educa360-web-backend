package com.educa360.backend.controller;

import com.educa360.backend.dto.ApoderadoRequest;
import com.educa360.backend.dto.UsuarioBasicoRequest;
import com.educa360.backend.dto.UsuarioResponse;
import com.educa360.backend.service.ApoderadoService;
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
 * Altas de APODERADO.
 *
 * Mismo criterio que EstudianteController: dominio de SECRETARIA con ADMIN
 * como superconjunto (la ruta = dominio responsable; el @PreAuthorize =
 * quién tiene permiso).
 *
 * Cuerpo: {"usuario": {...}, "perfil": {...}} — perfil vacío por ahora;
 * sus representados se gestionarán con el módulo de estudiantes.
 */
@RestController
@RequestMapping("/api/v1/secretaria/apoderados")
@PreAuthorize("hasAnyRole('ADMIN', 'SECRETARIA')")
public class ApoderadoController {

    private final ApoderadoService apoderadoService;

    public ApoderadoController(ApoderadoService apoderadoService) {
        this.apoderadoService = apoderadoService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(
            @Valid @RequestBody Cuerpo body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apoderadoService.crear(body.usuario(), body.perfil()));
    }

    /** Envoltorio del request: parte común (usuario) + parte del rol (perfil). */
    public record Cuerpo(
            @Valid UsuarioBasicoRequest usuario,
            @Valid @NotNull ApoderadoRequest perfil) {
    }
}
