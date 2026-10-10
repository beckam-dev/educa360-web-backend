package com.educa360.backend.exception;

import java.time.Duration;

/**
 * 429: se superó el límite de intentos fallidos de login.
 * La respuesta incluye el header Retry-After con los segundos de espera.
 */
public class RateLimitedException extends RuntimeException {

    private final Duration reintentarEn;

    public RateLimitedException(Duration reintentarEn) {
        super("Demasiados intentos fallidos de login");
        this.reintentarEn = reintentarEn;
    }

    public Duration getReintentarEn() {
        return reintentarEn;
    }
}
