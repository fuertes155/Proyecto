package com.cooperativa.met.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Servicio de idempotencia para operaciones financieras críticas.
 *
 * <p>Evita el doble procesamiento de transferencias cuando el cliente reintenta
 * una petición por fallo de red u otros errores transitorios.</p>
 *
 * <p>Clave Redis: {@code idempotency:{key}} → resultado serializado (24h TTL)</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);
    private static final String PROCESSING = "PROCESSING";

    private final StringRedisTemplate redisTemplate;

    /**
     * Intenta marcar la clave como "en proceso".
     *
     * @param key la clave de idempotencia del cliente
     * @return true si se pudo marcar (petición nueva), false si ya existe (repetida)
     */
    public boolean tryAcquire(String key) {
        try {
            Boolean set = redisTemplate.opsForValue()
                    .setIfAbsent(PREFIX + key, PROCESSING, TTL);
            return Boolean.TRUE.equals(set);
        } catch (Exception e) {
            log.warn("Error al consultar idempotencia para key={}: {}", key, e.getMessage());
            // fail-open: si Redis falla, permitimos continuar (el optimistic locking DB protege)
            return true;
        }
    }

    /**
     * Obtiene el resultado previamente guardado para una clave, si existe.
     *
     * @return Optional con el resultado anterior, o vacío si es primera ejecución
     */
    public Optional<String> getResult(String key) {
        try {
            String value = redisTemplate.opsForValue().get(PREFIX + key);
            if (value == null || PROCESSING.equals(value)) return Optional.empty();
            return Optional.of(value);
        } catch (Exception e) {
            log.warn("Error al obtener resultado idempotente para key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Guarda el resultado final de la operación asociado a la clave.
     *
     * @param key    la clave de idempotencia
     * @param result resultado serializado (ej. ID de transacción)
     */
    public void saveResult(String key, String result) {
        try {
            redisTemplate.opsForValue().set(PREFIX + key, result, TTL);
        } catch (Exception e) {
            log.warn("Error al guardar resultado idempotente key={}: {}", key, e.getMessage());
        }
    }
}
