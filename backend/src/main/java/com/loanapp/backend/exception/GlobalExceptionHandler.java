package com.loanapp.backend.exception;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getErrorCode(),
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ErrorResponse> handleTechnical(TechnicalException ex, HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.TECHNICAL_ERROR,
                ex.getMessage(),
                request
        );
    }

    // Caught @Valid errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("errors", fieldErrors);
        response.put("path", request.getRequestURI());

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PARAMETER, message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.UNEXPECTED_ERROR,
                "Unexpected server error",
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, ErrorCode errorCode,
                                                             String message, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .errorCode(String.valueOf(errorCode))
                .message(message)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(error);
    }
}
