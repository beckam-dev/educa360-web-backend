package com.educa360.backend.exception;

/**
 * 400: la petición es sintácticamente correcta pero viola una regla de
 * negocio (p. ej. "un estudiante no puede tener teléfonos").
 * Se distingue de la validación de DTO (@Valid), que Spring resuelve solo.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String mensaje) {
        super(mensaje);
    }
}
