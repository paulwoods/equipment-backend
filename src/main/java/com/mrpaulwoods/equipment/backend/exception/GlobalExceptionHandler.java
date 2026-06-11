package com.mrpaulwoods.equipment.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex,
                                                        jakarta.servlet.http.HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(), "Resource Not Found", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(ImportEquipmentException.class)
    public ResponseEntity<ProblemDetail> handleImportEquipment(ImportEquipmentException ex,
                                                               jakarta.servlet.http.HttpServletRequest request) {
        log.warn("Import error: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, ex.getMessage(), "Import Error", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex,
                                                          jakarta.servlet.http.HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> Objects.requireNonNullElse(fe.getDefaultMessage(), "Invalid value"),
                        (a, b) -> a
                ));
        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, "Validation failed", "Validation Error", request.getRequestURI());
        problem.setProperty("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLocking(OptimisticLockingFailureException ex,
                                                                 jakarta.servlet.http.HttpServletRequest request) {
        log.warn("Optimistic locking conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetails.of(HttpStatus.CONFLICT,
                "The record was modified by another user. Reload and try again.",
                "Concurrent Modification", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException ex,
                                                              jakarta.servlet.http.HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemDetail problem = ProblemDetails.of(status, ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(), status.getReasonPhrase(), request.getRequestURI());
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex,
                                                       jakarta.servlet.http.HttpServletRequest request) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetails.of(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", "Internal Server Error", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
