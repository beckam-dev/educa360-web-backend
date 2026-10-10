package com.educa360.backend.controller;

import com.educa360.backend.dto.UsuarioCreateRequest;
import com.educa360.backend.dto.UsuarioEstadoRequest;
import com.educa360.backend.dto.UsuarioPasswordRequest;
import com.educa360.backend.dto.UsuarioResponse;
import com.educa360.backend.dto.UsuarioUpdateRequest;
import com.educa360.backend.entity.User;
import com.educa360.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestión de usuarios y perfiles. Exclusiva de ADMIN.
 *
 * El rol del request determina el perfil que se crea (consistencia
 * perfil ↔ rol garantizada en UsuarioService).
 */
@RestController
@RequestMapping("/api/v1/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** GET /api/v1/admin/usuarios?rol=DOCENTE&activo=true&q=perez */
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar(
            @RequestParam(required = false) User.Role rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(usuarioService.listar(rol, activo, q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtener(id));
    }

    /** POST /api/v1/admin/usuarios -> crea la cuenta Y su perfil según el rol */
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest request) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request));
    }

    /** PATCH /{id}/activo -> baja lógica (corta el acceso al instante) */
    @PatchMapping("/{id}/activo")
    public ResponseEntity<UsuarioResponse> cambiarActivo(
            @PathVariable Long id, @Valid @RequestBody UsuarioEstadoRequest request) {
        return ResponseEntity.ok(usuarioService.cambiarActivo(id, request.activo()));
    }

    /** PATCH /{id}/password -> reseteo por ADMIN */
    @PatchMapping("/{id}/password")
    public ResponseEntity<UsuarioResponse> restablecerPassword(
            @PathVariable Long id, @Valid @RequestBody UsuarioPasswordRequest request) {
        return ResponseEntity.ok(usuarioService.restablecerPassword(id, request.password()));
    }
}
