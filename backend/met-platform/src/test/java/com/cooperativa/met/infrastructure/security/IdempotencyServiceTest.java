package com.cooperativa.met.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks
    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── tryAcquire ────────────────────────────────────────────────────────────

    @Test
    void tryAcquire_returnsTrue_forNewKey() {
        // Primera vez que se ve esta clave → Redis devuelve true (setIfAbsent exitoso)
        when(valueOps.setIfAbsent(eq("idempotency:idem-001"), eq("PROCESSING"), eq(Duration.ofHours(24))))
                .thenReturn(true);

        boolean result = service.tryAcquire("idem-001");

        assertTrue(result);
    }

    @Test
    void tryAcquire_returnsFalse_forDuplicateKey() {
        // Clave ya existe → Redis devuelve false (setIfAbsent falla)
        when(valueOps.setIfAbsent(eq("idempotency:idem-001"), any(), any(Duration.class)))
                .thenReturn(false);

        boolean result = service.tryAcquire("idem-001");

        assertFalse(result);
    }

    @Test
    void tryAcquire_returnsTrue_whenRedisThrowsException_failOpenBehavior() {
        // Si Redis falla, permitimos continuar (fail-open) para no bloquear operaciones
        when(valueOps.setIfAbsent(any(), any(), any(Duration.class)))
                .thenThrow(new RuntimeException("Redis connection refused"));

        boolean result = service.tryAcquire("idem-fallback");

        assertTrue(result, "Debe devolver true (fail-open) cuando Redis no está disponible");
    }

    @Test
    void tryAcquire_returnsFalse_whenRedisReturnsNull() {
        // setIfAbsent puede devolver null en casos edge
        when(valueOps.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(null);

        boolean result = service.tryAcquire("idem-null");

        // Boolean.TRUE.equals(null) == false
        assertFalse(result);
    }

    // ── getResult ─────────────────────────────────────────────────────────────

    @Test
    void getResult_returnsEmpty_whenKeyIsProcessing() {
        // La clave existe pero está en estado PROCESSING (aún no terminó)
        when(valueOps.get("idempotency:idem-001")).thenReturn("PROCESSING");

        Optional<String> result = service.getResult("idem-001");

        assertTrue(result.isEmpty());
    }

    @Test
    void getResult_returnsEmpty_whenKeyDoesNotExist() {
        when(valueOps.get("idempotency:idem-new")).thenReturn(null);

        Optional<String> result = service.getResult("idem-new");

        assertTrue(result.isEmpty());
    }

    @Test
    void getResult_returnsValue_whenResultWasSaved() {
        // La operación ya completó y guardó el ID de transacción
        when(valueOps.get("idempotency:idem-done")).thenReturn("txn-uuid-abc123");

        Optional<String> result = service.getResult("idem-done");

        assertTrue(result.isPresent());
        assertEquals("txn-uuid-abc123", result.get());
    }

    @Test
    void getResult_returnsEmpty_whenRedisThrowsException() {
        // Si Redis falla al consultar, devolvemos empty (no bloqueamos)
        when(valueOps.get(any())).thenThrow(new RuntimeException("Redis timeout"));

        Optional<String> result = service.getResult("idem-error");

        assertTrue(result.isEmpty());
    }

    // ── saveResult ────────────────────────────────────────────────────────────

    @Test
    void saveResult_callsRedisSetWithCorrectKeyAndTtl() {
        service.saveResult("idem-001", "txn-12345");

        verify(valueOps).set("idempotency:idem-001", "txn-12345", Duration.ofHours(24));
    }

    @Test
    void saveResult_doesNotThrow_whenRedisIsUnavailable() {
        // Si Redis falla al guardar, no propagamos la excepción (operación ya completó)
        doThrow(new RuntimeException("Redis down")).when(valueOps)
                .set(any(), any(), any(Duration.class));

        assertDoesNotThrow(() -> service.saveResult("idem-001", "txn-12345"));
    }

    // ── Flujo completo ────────────────────────────────────────────────────────

    @Test
    void fullFlow_acquireThenSaveThenRetrieve() {
        String key = "idem-transfer-flow";
        String transactionId = "txn-flow-999";

        // 1. Primera petición: adquiere el lock
        when(valueOps.setIfAbsent(eq("idempotency:" + key), any(), any())).thenReturn(true);
        assertTrue(service.tryAcquire(key));

        // 2. Petición duplicada mientras procesa: ya existe la clave
        when(valueOps.setIfAbsent(eq("idempotency:" + key), any(), any())).thenReturn(false);
        assertFalse(service.tryAcquire(key));

        // 3. Resultado guardado → aún en PROCESSING, no disponible
        when(valueOps.get("idempotency:" + key)).thenReturn("PROCESSING");
        assertTrue(service.getResult(key).isEmpty());

        // 4. Operación completa → resultado disponible
        when(valueOps.get("idempotency:" + key)).thenReturn(transactionId);
        Optional<String> saved = service.getResult(key);
        assertTrue(saved.isPresent());
        assertEquals(transactionId, saved.get());
    }
}
