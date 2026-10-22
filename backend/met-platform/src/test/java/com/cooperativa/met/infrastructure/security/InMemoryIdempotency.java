package com.cooperativa.met.infrastructure.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * {@link IdempotencyService} real sobre un Redis en memoria: permite probar reintentos con la misma
 * clave (los tests de integración usan un Redis simulado que siempre "falla abierto").
 */
public final class InMemoryIdempotency {

    private InMemoryIdempotency() {
    }

    @SuppressWarnings("unchecked")
    public static IdempotencyService service() {
        Map<String, String> store = new ConcurrentHashMap<>();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);

        lenient().when(redis.opsForValue()).thenReturn(values);
        lenient().when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(inv -> store.putIfAbsent(inv.getArgument(0), inv.getArgument(1)) == null);
        lenient().when(values.get(any())).thenAnswer(inv -> store.get(inv.<String>getArgument(0)));
        lenient().doAnswer(inv -> store.put(inv.getArgument(0), inv.getArgument(1)))
                .when(values).set(anyString(), anyString(), any(Duration.class));
        lenient().when(redis.delete(anyString())).thenAnswer(inv -> store.remove(inv.<String>getArgument(0)) != null);

        return new IdempotencyService(redis);
    }
}
