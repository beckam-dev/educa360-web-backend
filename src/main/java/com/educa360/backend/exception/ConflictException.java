package com.educa360.backend.exception;

/**
 * Conflicto de recurso (HTTP 409): email/DNI duplicado, entidad en uso, etc.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
