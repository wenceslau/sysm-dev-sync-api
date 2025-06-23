package com.sysm.devsync.infrastructure.controller;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.NotFoundException;
import com.sysm.devsync.infrastructure.controller.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles exceptions from @Valid annotations, returning a detailed list of field errors.
     *
     * @return ResponseEntity with status 400 (Bad Request).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, List<String>> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
        });

        var errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Validation failed for one or more fields",
                request.getRequestURI(),
                errors
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles custom business logic exceptions, like an invalid search field.
     *
     * @return ResponseEntity with status 400 (Bad Request).
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        final HttpStatus status = HttpStatus.BAD_REQUEST;
        var errorResponse = new ErrorResponse(
                Instant.now(),
                status.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Handles specific "Not Found" exceptions when a resource cannot be found.
     *
     * @return ResponseEntity with status 404 (Not Found).
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
        final HttpStatus status  = HttpStatus.NOT_FOUND;

        var errorResponse = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Handles generic argument validation exceptions (e.g., invalid query parameters).
     * This no longer needs to check for "not found" messages.
     *
     * @return ResponseEntity with status 400 (Bad Request).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        final HttpStatus status = HttpStatus.BAD_REQUEST;

        var errorResponse = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * A catch-all handler for any other unhandled exceptions.
     * This prevents stack traces from being exposed to the client.
     *
     * @return ResponseEntity with status 500 (Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        // Log the full stack trace for debugging, but don't expose it to the client
        log.error("An unexpected error occurred at path: {}", request.getRequestURI(), ex);

        final HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        var errorResponse = new ErrorResponse(
                Instant.now(),
                status.value(),
                "Internal Server Error",
                "An unexpected internal error occurred. Please try again later.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, status);
    }
}
