package com.cooperativa.met.infrastructure.security;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * Bloqueo por cuenta para el login de administradores: tras 5 contraseñas incorrectas la cuenta
 * queda bloqueada 15 minutos. El límite por IP de {@link AuthRateLimitFilter} no basta, porque la
 * IP sale de X-Forwarded-For y un atacante puede cambiarla en cada intento.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminLoginAttemptService {

    private static final String PREFIX = "admin-login:attempts:";
    private static final String BLOCK_PREFIX = "admin-login:blocked:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration ATTEMPT_WINDOW = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    public void checkBlocked(String username) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(BLOCK_PREFIX + key(username)))) {
            throw new BusinessRuleException("ADMIN_LOCKED",
                    "La cuenta está bloqueada temporalmente por múltiples intentos fallidos. Intenta en 15 minutos.");
        }
    }

    public void recordFailure(String username) {
        String attemptKey = PREFIX + key(username);
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptKey, ATTEMPT_WINDOW);
        }
        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            redisTemplate.opsForValue().set(BLOCK_PREFIX + key(username), "1", BLOCK_DURATION);
            redisTemplate.delete(attemptKey);
            log.warn("Cuenta de administrador '{}' bloqueada por {} intentos fallidos de login.", key(username), attempts);
        }
    }

    public void resetAttempts(String username) {
        redisTemplate.delete(PREFIX + key(username));
    }

    // Sin distinguir mayúsculas ni espacios, para que "Admin " no cuente como otra cuenta.
    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
