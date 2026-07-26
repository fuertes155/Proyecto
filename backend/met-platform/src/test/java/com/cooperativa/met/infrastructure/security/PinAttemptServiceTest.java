package com.cooperativa.met.infrastructure.security;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PinAttemptServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks
    private PinAttemptService service;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── checkAndRecordFailure ─────────────────────────────────────────────────

    @Test
    void checkAndRecordFailure_throwsInvalidPin_onFirstFourAttempts() {
        // La cuenta NO está bloqueada, primer intento (count=1)
        when(redisTemplate.hasKey("pin:blocked:" + userId)).thenReturn(false);
        when(valueOps.increment("pin:attempts:" + userId)).thenReturn(1L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.checkAndRecordFailure(userId));

        assertEquals("INVALID_PIN", ex.getCode());
        assertTrue(ex.getMessage().contains("1 de 5"));
    }

    @Test
    void checkAndRecordFailure_throwsInvalidPin_onFourthAttempt() {
        when(redisTemplate.hasKey("pin:blocked:" + userId)).thenReturn(false);
        when(valueOps.increment("pin:attempts:" + userId)).thenReturn(4L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.checkAndRecordFailure(userId));

        assertEquals("INVALID_PIN", ex.getCode());
        assertTrue(ex.getMessage().contains("4 de 5"));
        // No debe bloquear todavía
        verify(valueOps, never()).set(contains("pin:blocked:"), any(), any(Duration.class));
    }

    @Test
    void checkAndRecordFailure_throwsAccountLocked_onFifthAttempt() {
        // 5° intento: debe bloquear la cuenta
        when(redisTemplate.hasKey("pin:blocked:" + userId)).thenReturn(false);
        when(valueOps.increment("pin:attempts:" + userId)).thenReturn(5L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.checkAndRecordFailure(userId));

        assertEquals("ACCOUNT_LOCKED", ex.getCode());
        // Verifica que se guardó la clave de bloqueo con TTL de 15 minutos
        verify(valueOps).set(eq("pin:blocked:" + userId), eq("1"), eq(Duration.ofMinutes(15)));
        // Verifica que se borró el contador de intentos
        verify(redisTemplate).delete("pin:attempts:" + userId);
    }

    @Test
    void checkAndRecordFailure_throwsAccountLocked_whenAlreadyBlocked() {
        // La cuenta ya está bloqueada — debe fallar inmediatamente sin incrementar
        when(redisTemplate.hasKey("pin:blocked:" + userId)).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.checkAndRecordFailure(userId));

        assertEquals("ACCOUNT_LOCKED", ex.getCode());
        // No debe incrementar el contador si ya está bloqueado
        verify(valueOps, never()).increment(any());
    }

    @Test
    void checkAndRecordFailure_setsExpiry_onFirstAttempt() {
        // El primer intento debe setear el TTL de la ventana (1 hora)
        when(redisTemplate.hasKey("pin:blocked:" + userId)).thenReturn(false);
        when(valueOps.increment("pin:attempts:" + userId)).thenReturn(1L);

        // No nos importa la excepción aquí, solo verificar el expire
        assertThrows(BusinessRuleException.class, () -> service.checkAndRecordFailure(userId));

        verify(redisTemplate).expire("pin:attempts:" + userId, Duration.ofHours(1));
    }

    @Test
    void checkAndRecordFailure_doesNotSetExpiry_onSubsequentAttempts() {
        // En el segundo intento (count != 1), no se debe volver a setear el TTL
        when(redisTemplate.hasKey("pin:blocked:" + userId)).thenReturn(false);
        when(valueOps.increment("pin:attempts:" + userId)).thenReturn(2L);

        assertThrows(BusinessRuleException.class, () -> service.checkAndRecordFailure(userId));

        verify(redisTemplate, never()).expire(any(), any(Duration.class));
    }

    // ── checkBlocked ──────────────────────────────────────────────────────────

    @Test
    void checkBlocked_doesNotThrow_whenNotBlocked() {
        when(redisTemplate.hasKey("pin:blocked:" + userId)).thenReturn(false);

        assertDoesNotThrow(() -> service.checkBlocked(userId));
    }

    @Test
    void checkBlocked_throwsAccountLocked_whenBlocked() {
        when(redisTemplate.hasKey("pin:blocked:" + userId)).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.checkBlocked(userId));

        assertEquals("ACCOUNT_LOCKED", ex.getCode());
    }

    // ── resetAttempts ─────────────────────────────────────────────────────────

    @Test
    void resetAttempts_deletesAttemptKey() {
        service.resetAttempts(userId);

        verify(redisTemplate).delete("pin:attempts:" + userId);
    }
}
