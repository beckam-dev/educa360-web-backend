package com.educa360.backend.dto;

/**
 * Respuesta estándar de autenticación.
 *
 * @param token       JWT firmado (usar en el header Authorization: Bearer <token>)
 * @param tokenType   Siempre "Bearer"
 * @param expiresIn   Validez del token en SEGUNDOS
 * @param user        Perfil/rol del usuario autenticado
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static AuthResponse bearer(String token, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, user);
    }
}
