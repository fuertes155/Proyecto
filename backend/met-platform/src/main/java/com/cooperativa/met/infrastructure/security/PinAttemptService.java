package com.cooperativa.met.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Servicio para controlar los intentos fallidos de PIN en operaciones sensibles.
 * Si un usuario falla 5 veces, se bloquea la cuenta durante 15 minutos para prevenir fuerza bruta.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PinAttemptService {

    private static final String PREFIX = "pin:attempts:";
    private static final String BLOCK_PREFIX = "pin:blocked:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration ATTEMPT_WINDOW = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    public void checkAndRecordFailure(UUID userId) {
        String blockKey = BLOCK_PREFIX + userId;
        String attemptKey = PREFIX + userId;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(blockKey))) {
            throw new com.cooperativa.met.domain.common.exception.BusinessRuleException(
                    "ACCOUNT_LOCKED", "Tu cuenta está bloqueada temporalmente por múltiples intentos fallidos de PIN. Intenta en 15 minutos.");
        }

        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptKey, ATTEMPT_WINDOW);
        }

        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            redisTemplate.opsForValue().set(blockKey, "1", BLOCK_DURATION);
            redisTemplate.delete(attemptKey);
            log.warn("Usuario {} bloqueado temporalmente por {} intentos fallidos de PIN.", userId, attempts);
            throw new com.cooperativa.met.domain.common.exception.BusinessRuleException(
                    "ACCOUNT_LOCKED", "Has excedido el número de intentos permitidos. Tu cuenta ha sido bloqueada por 15 minutos.");
        } else {
            throw new com.cooperativa.met.domain.common.exception.BusinessRuleException(
                    "INVALID_PIN", "El PIN ingresado es incorrecto. Intento " + attempts + " de " + MAX_ATTEMPTS);
        }
    }

    public void checkBlocked(UUID userId) {
        String blockKey = BLOCK_PREFIX + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blockKey))) {
            throw new com.cooperativa.met.domain.common.exception.BusinessRuleException(
                    "ACCOUNT_LOCKED", "Tu cuenta está bloqueada temporalmente por múltiples intentos fallidos de PIN. Intenta en 15 minutos.");
        }
    }

    public void resetAttempts(UUID userId) {
        redisTemplate.delete(PREFIX + userId);
    }
}
