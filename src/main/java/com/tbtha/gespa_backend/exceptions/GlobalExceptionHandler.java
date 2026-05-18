package com.tbtha.gespa_backend.exceptions;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toMessage)
                .toList();

        ApiError error = new ApiError(
                "VALIDATION_ERROR",
                "La solicitud contiene errores de validación",
                details,
                OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        ApiError error = new ApiError(
                "NOT_FOUND",
                ex.getMessage(),
                List.of(),
                OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        ApiError error = new ApiError(
                "CONFLICT",
                ex.getMessage(),
                List.of(),
                OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        ApiError error = new ApiError(
                "UNAUTHORIZED",
                "Credenciales inválidas",
                List.of(),
                OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        ApiError error = new ApiError(
                "FORBIDDEN",
                ex.getMessage(),
                List.of(),
                OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex) {
        ApiError error = new ApiError(
                "NOT_FOUND",
                "Recurso no encontrado: " + ex.getResourcePath(),
                List.of(),
                OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<ApiError> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex,
                                       HttpServletRequest request) {
        ApiError error = new ApiError(
                "UNSUPPORTED_MEDIA_TYPE",
                "Content-Type no soportado. Usa application/json",
            List.of(
                "path=" + request.getRequestURI(),
                "contentType=" + String.valueOf(request.getContentType()),
                ex.getMessage() == null ? "Sin detalle" : ex.getMessage()
            ),
                OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        ApiError error = new ApiError(
                "INTERNAL_ERROR",
                "Ocurrió un error inesperado",
                List.of(ex.getMessage() == null ? "Sin detalle" : ex.getMessage()),
                OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private String toMessage(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
