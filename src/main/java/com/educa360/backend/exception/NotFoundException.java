package com.educa360.backend.exception;

/** 404: el recurso pedido no existe (id inexistente, por ejemplo). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String mensaje) {
        super(mensaje);
    }
}
