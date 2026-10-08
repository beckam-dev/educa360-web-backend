package com.educa360.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints de prueba para verificar el RBAC desde el frontend.
 * Borrar cuando existan los módulos reales.
 */
@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    /** Cualquier usuario autenticado. */
    @GetMapping("/authenticated")
    public ResponseEntity<Map<String, String>> authenticated(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "message", "Hola " + authentication.getName() + ", estás autenticado",
                "roles", authentication.getAuthorities().toString()
        ));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> admin() {
        return ResponseEntity.ok(Map.of("message", "Zona ADMIN: estructura académica y reportes"));
    }

    @GetMapping("/secretaria")
    @PreAuthorize("hasRole('SECRETARIA')")
    public ResponseEntity<Map<String, String>> secretaria() {
        return ResponseEntity.ok(Map.of("message", "Zona SECRETARIA: tickets, matrículas y estados"));
    }

    @GetMapping("/docente")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<Map<String, String>> docente() {
        return ResponseEntity.ok(Map.of("message", "Zona DOCENTE: asistencia y calificaciones"));
    }

    @GetMapping("/estudiante")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Map<String, String>> estudiante() {
        return ResponseEntity.ok(Map.of("message", "Zona ESTUDIANTE: mis notas y asistencias"));
    }

    @GetMapping("/apoderado")
    @PreAuthorize("hasRole('APODERADO')")
    public ResponseEntity<Map<String, String>> apoderado() {
        return ResponseEntity.ok(Map.of("message", "Zona APODERADO: seguimiento de mis representados"));
    }
}
