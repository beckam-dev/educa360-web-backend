package com.educa360.backend.controller;

import com.educa360.backend.dto.HijoResponse;
import com.educa360.backend.service.ApoderadoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Zona del APODERADO: SUS hijos (los formalmente asignados, §5 acceso por
 * perfil). La identidad sale del token: no recibe ids, no puede pedir los
 * de nadie más. El vínculo lo crea Secretaría — aquí sólo se consulta.
 */
@RestController
@RequestMapping("/api/v1/apoderado/hijos")
@PreAuthorize("hasRole('APODERADO')")
public class ApoderadoHijosController {

    private final ApoderadoService apoderadoService;

    public ApoderadoHijosController(ApoderadoService apoderadoService) {
        this.apoderadoService = apoderadoService;
    }

    /** Representados con su sección del período vigente (null si aún sin matrícula). */
    @GetMapping
    public ResponseEntity<List<HijoResponse>> misHijos(Authentication authentication) {
        return ResponseEntity.ok(apoderadoService.listarHijosDelUsuario(authentication.getName()));
    }
}
