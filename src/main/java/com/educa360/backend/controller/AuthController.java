package com.educa360.backend.controller;

import com.educa360.backend.dto.AuthResponse;
import com.educa360.backend.dto.LoginRequest;
import com.educa360.backend.dto.RegisterRequest;
import com.educa360.backend.dto.UserResponse;
import com.educa360.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/v1/auth/login -> JWT + perfil
     *
     * La IP acompaña al email en el límite de intentos fallidos.
     * getRemoteAddr() es la IP real salvo que haya un proxy delante: en ese
     * caso activar server.forward-headers-strategy=framework (leer el
     * X-Forwarded-For a mano sería manipulable por el cliente).
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest.getRemoteAddr()));
    }

    /** POST /api/v1/auth/register -> pública sólo hasta crear la primera cuenta */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /** GET /api/v1/auth/me -> perfil del usuario del token */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.me(authentication.getName()));
    }
}
