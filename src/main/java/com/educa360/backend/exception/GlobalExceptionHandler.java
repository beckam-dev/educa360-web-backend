package com.educa360.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un único lugar donde la API traduce excepciones a respuestas JSON.
 * Nunca filtra stack trace al cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 400: validación de los DTO (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.put(error.getField(), error.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos", request, campos);
    }

    // 403: la cuenta existe pero está dada de baja
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Usuario inactivo: contacta con la administración", request, null);
    }

    // 401: email o contraseña incorrectos
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiError> handleBadCredentials(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", request, null);
    }

    // 401: cualquier otro fallo de autenticación
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    // 403: autenticado pero sin permiso (@PreAuthorize / bootstrap de registro)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    // 409: email/DNI ya registrados (regla de negocio)
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // 409: violación de constraints de la BD (red de seguridad)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Violación de integridad de datos: el email o el DNI ya existen", request, null);
    }

    // 500: todo lo no previsto
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request,
                                           Map<String, String> fields) {
        ApiError body = fields == null
                ? ApiError.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI())
                : ApiError.withFields(status.value(), status.getReasonPhrase(), message, request.getRequestURI(), fields);
        return ResponseEntity.status(status).body(body);
    }
}
