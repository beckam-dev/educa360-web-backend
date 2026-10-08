package com.educa360.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un único lugar donde la API traduce excepciones a respuestas JSON.
 *
 * Hereda de ResponseEntityExceptionHandler para que TODAS las excepciones
 * internas de Spring MVC conserven su código correcto (400, 404, 405, 415...)
 * y sólo se sustituya el cuerpo por nuestro ApiError. Sin este extender,
 * un catch-all Exception devolvería 500 para cualquier cosa.
 *
 * Nunca se filtra un stack trace al cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ==================== Errores propios de negocio / seguridad ====================

    // 403: la cuenta existe pero está dada de baja
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Object> handleDisabled(DisabledException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Usuario inactivo: contacta con la administración", request, null);
    }

    // 401: email o contraseña incorrectos (mismo mensaje: sin enumeración de usuarios)
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<Object> handleBadCredentials(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", request, null);
    }

    // 401: cualquier otro fallo de autenticación
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    // 403: autenticado pero sin permiso (@PreAuthorize / alta inicial de registro)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    // 409: email/DNI ya registrados (regla de negocio)
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // 409: violación de constraints de la BD (red de seguridad).
    // Se identifica el constraint por la columna del DETAIL de Postgres
    // ("Key (email)=(...) already exists") sin filtrar el SQL al cliente.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String causa = String.valueOf(ex.getMostSpecificCause().getMessage());
        String mensaje = "Violación de integridad de datos";
        if (causa.contains("(email)=")) {
            mensaje = "El email ya está registrado";
        } else if (causa.contains("(dni)=")) {
            mensaje = "El DNI ya está registrado";
        } else if (causa.contains("(user_id)=")) {
            mensaje = "La relación con la cuenta de usuario ya existe";
        }
        return build(HttpStatus.CONFLICT, mensaje, request, null);
    }

    // 500: todo lo no previsto (los errores de Spring MVC NO llegan aquí:
    // los resuelve la clase base con su código correcto)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request, null);
    }

    // ==================== Excepciones de Spring MVC (códigos correctos) ====================

    /**
     * Punto único de salida de la clase base: aquí se garantiza que todos
     * los errores de Spring (404, 405, 415, 400...) se devuelvan como ApiError.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode,
            WebRequest request) {

        ApiError error = (body instanceof ApiError existing)
                ? existing
                : ApiError.of(statusCode.value(), reason(statusCode), mensaje(ex), path(request));

        return super.handleExceptionInternal(ex, error, headers, statusCode, request);
    }

    /** 400 con detalle campo a campo de los DTO validados con @Valid. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode statusCode,
            WebRequest request) {

        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> campos.put(error.getField(), error.getDefaultMessage()));

        ApiError body = ApiError.withFields(400, reason(HttpStatus.BAD_REQUEST),
                "Datos de entrada inválidos", path(request), campos);
        return handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    // ==================== Helpers ====================

    private ResponseEntity<Object> build(HttpStatus status, String message,
                                         HttpServletRequest request, Map<String, String> fields) {
        ApiError body = fields == null
                ? ApiError.of(status.value(), status.getReasonPhrase(), message, path(request))
                : ApiError.withFields(status.value(), status.getReasonPhrase(), message, path(request), fields);
        return ResponseEntity.status(status).body(body);
    }

    private String mensaje(Exception ex) {
        return switch (ex) {
            case HttpMessageNotReadableException e ->
                    "Cuerpo de la petición ilegible, mal formado o con tipos de datos inválidos";
            case HttpRequestMethodNotSupportedException e ->
                    "Método " + e.getMethod() + " no permitido en este recurso";
            case HttpMediaTypeNotSupportedException e -> "Content-Type no soportado en este endpoint";
            case MethodArgumentTypeMismatchException e ->
                    "Valor inválido para el parámetro '" + e.getName() + "'";
            case NoResourceFoundException e -> "Recurso no encontrado: " + e.getResourcePath();
            default -> ex.getMessage();
        };
    }

    private String reason(HttpStatusCode statusCode) {
        return HttpStatus.valueOf(statusCode.value()).getReasonPhrase();
    }

    private String path(HttpServletRequest request) {
        return request.getRequestURI();
    }

    /** Spring 7 describe la ruta como "uri=/api/..." */
    private String path(WebRequest request) {
        String description = request.getDescription(false);
        return description.startsWith("uri=") ? description.substring("uri=".length()) : description;
    }
}
