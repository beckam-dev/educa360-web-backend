package com.educa360.backend.dto;

/**
 * GET /auth/me: todo lo que la pantalla de perfil necesita en UNA llamada —
 * la cuenta (misma vista que el login) + el perfil de su rol.
 * Sin esto el front tendría que hacer N peticiones o asumir ids.
 */
public record MeResponse(UserResponse usuario, MePerfilResponse perfil) {
}
