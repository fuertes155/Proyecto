package com.cooperativa.fintech.infrastructure.web.common;

import com.cooperativa.fintech.domain.common.exception.BusinessRuleException;
import com.cooperativa.fintech.domain.common.exception.DomainException;
import com.cooperativa.fintech.domain.common.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessRuleException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }
        ApiErrorResponse body = new ApiErrorResponse(
                "VALIDATION_ERROR",
                "Datos de entrada inválidos",
                Instant.now(),
                fields
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        ApiErrorResponse body = new ApiErrorResponse(
                "INTERNAL_ERROR",
                "Error interno del servidor",
                Instant.now(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, DomainException ex) {
        ApiErrorResponse body = new ApiErrorResponse(ex.getCode(), ex.getMessage(), Instant.now(), null);
        return ResponseEntity.status(status).body(body);
    }

    public record ApiErrorResponse(
            String code,
            String message,
            Instant timestamp,
            Map<String, String> fields
    ) {
    }
}
