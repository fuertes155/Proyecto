package com.cooperativa.met.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Lista negra de access tokens JWT revocados.
 *
 * <p>Cuando un usuario hace logout, su access token se agrega aquí con un TTL
 * igual al tiempo restante del token. El {@link JwtAuthenticationFilter} consulta
 * esta lista antes de autenticar cualquier petición.</p>
 *
 * <p>Clave Redis: {@code jwt:blacklist:{token}} → valor: "1"</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Agrega un token a la lista negra.
     *
     * @param token     El access token JWT completo
     * @param ttl       Tiempo restante de validez del token (para fijar el TTL en Redis)
     */
    public void revoke(String token, Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) return; // ya expiró, no hace falta
        try {
            redisTemplate.opsForValue().set(PREFIX + token, "1", ttl);
            log.debug("Token agregado a lista negra. TTL={}s", ttl.getSeconds());
        } catch (Exception e) {
            log.warn("No se pudo agregar token a lista negra: {}", e.getMessage());
        }
    }

    /**
     * Verifica si un token está revocado.
     *
     * @return true si el token está en la lista negra
     */
    public boolean isRevoked(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
        } catch (Exception e) {
            log.warn("Error al consultar lista negra: {}", e.getMessage());
            return false; // fail-open: si Redis falla, no bloqueamos
        }
    }
}
