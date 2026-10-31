package com.cooperativa.met.infrastructure.web.common;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.DomainException;
import com.cooperativa.met.domain.common.exception.RateLimitExceededException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.infrastructure.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final RateLimitProperties rateLimitProperties;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        ApiErrorResponse body = new ApiErrorResponse(ex.getCode(), ex.getMessage(), Instant.now(), null);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(rateLimitProperties.getWindowSeconds()))
                .body(body);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessRuleException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ApiErrorResponse body = new ApiErrorResponse(
                "BAD_REQUEST",
                ex.getMessage(),
                Instant.now(),
                null
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
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

    /** Cuerpo ilegible: JSON mal escrito o un valor fuera de su lista (p. ej. un tipo de documento que no existe). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("Cuerpo de la petición ilegible: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "BAD_REQUEST", "Los datos enviados no tienen un formato válido", Instant.now(), null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "BAD_REQUEST", "El valor de '" + ex.getName() + "' no es válido", Instant.now(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        // Spring ya clasifica sus propios errores de quien llama (ruta inexistente, método no permitido,
        // falta una cabecera, tipo de contenido no soportado): responden con su 4xx y no son fallas del servidor.
        if (ex instanceof ErrorResponse springError && springError.getStatusCode().is4xxClientError()) {
            log.debug("Petición rechazada con {}: {}", springError.getStatusCode().value(), ex.getMessage());
            return ResponseEntity.status(springError.getStatusCode())
                    .headers(springError.getHeaders())
                    .body(clientError(springError.getStatusCode()));
        }
        log.error("Error interno no controlado: {}", ex.getMessage(), ex);
        ApiErrorResponse body = new ApiErrorResponse(
                "INTERNAL_ERROR",
                "Error interno del servidor",
                Instant.now(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private static ApiErrorResponse clientError(HttpStatusCode status) {
        String message = switch (status.value()) {
            case 404 -> "El recurso solicitado no existe";
            case 405 -> "Esta operación no está permitida en esta ruta";
            case 415 -> "El tipo de contenido enviado no es compatible";
            default -> "La petición está incompleta o no es válida";
        };
        HttpStatus known = HttpStatus.resolve(status.value());
        return new ApiErrorResponse(known != null ? known.name() : "BAD_REQUEST", message, Instant.now(), null);
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
