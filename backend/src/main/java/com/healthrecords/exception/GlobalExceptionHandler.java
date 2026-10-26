package com.healthrecords.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Single place for API error responses. The shape is deliberately stable:
 * every body has status, error, timestamp and path, and validation errors
 * additionally carry a fields map — the React frontend reads data.error
 * and data.fields, so those keys must not be renamed.
 * Internal exception messages are never leaked to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(v ->
                fieldErrors.put(v.getPropertyPath().toString(), v.getMessage()));
        Map<String, Object> body = base(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed", request);
        body.put("fields", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleDenied(AccessDeniedException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "You do not have permission to access this resource.", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        // Deliberately generic: do not reveal whether the email exists.
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid email or password.", request);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, Object>> handleLocked(LockedException ex, HttpServletRequest request) {
        // 423 Locked, with a clear (not generic) message: the user needs to
        // know they are locked out and that it is temporary.
        return error(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                "Account is temporarily locked after too many failed login attempts. Please try again later.",
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "CONFLICT",
                "The request conflicts with existing data (for example, a duplicate email).", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUpload(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "Uploaded file exceeds the configured maximum size.", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "Missing required parameter: " + ex.getParameterName(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));
        Map<String, Object> body = base(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed", request);
        body.put("fields", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred.", request);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message,
                                                      HttpServletRequest request) {
        return ResponseEntity.status(status).body(base(status, code, message, request));
    }

    private Map<String, Object> base(HttpStatus status, String code, String message, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("code", code);
        body.put("error", message);
        body.put("timestamp", Instant.now().toString());
        body.put("path", request != null ? request.getRequestURI() : null);
        return body;
    }
}
