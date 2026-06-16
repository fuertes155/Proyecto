package com.cooperativa.met.domain.common.exception;

/**
 * Excepción lanzada cuando se supera el límite de solicitudes permitidas
 * en un período de tiempo determinado.
 */
public class RateLimitExceededException extends DomainException {

    public RateLimitExceededException() {
        super("RATE_LIMIT_EXCEEDED",
              "Demasiados intentos. Por favor espera un momento antes de intentar de nuevo.");
    }
}
