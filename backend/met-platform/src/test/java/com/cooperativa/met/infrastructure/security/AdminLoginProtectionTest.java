package com.cooperativa.met.infrastructure.security;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.infrastructure.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fuerza bruta contra el login de administradores: el filtro por IP también debe cubrir
 * /v1/admin/auth/login, y como la IP sale de X-Forwarded-For (que el cliente controla), además
 * hace falta un bloqueo por cuenta que no dependa de la IP.
 */
@ExtendWith(MockitoExtension.class)
class AdminLoginProtectionTest {

    @Mock private RedisRateLimiter redisRateLimiter;
    @Mock private FilterChain chain;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private final Map<String, String> redis = new HashMap<>();
    private final Map<String, Duration> ttls = new HashMap<>();

    @BeforeEach
    void fakeRedis() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.increment(anyString())).thenAnswer(inv -> {
            long next = Long.parseLong(redis.getOrDefault(inv.<String>getArgument(0), "0")) + 1;
            redis.put(inv.getArgument(0), String.valueOf(next));
            return next;
        });
        lenient().doAnswer(inv -> {
            redis.put(inv.getArgument(0), inv.getArgument(1));
            ttls.put(inv.getArgument(0), inv.getArgument(2));
            return null;
        }).when(valueOps).set(anyString(), anyString(), any(Duration.class));
        lenient().when(redisTemplate.hasKey(anyString())).thenAnswer(inv -> redis.containsKey(inv.<String>getArgument(0)));
        lenient().when(redisTemplate.delete(anyString())).thenAnswer(inv -> redis.remove(inv.<String>getArgument(0)) != null);
    }

    @Test
    void shouldRateLimitTheAdminLoginLikeTheMemberLogin() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new RateLimitProperties(), new ObjectMapper(), redisRateLimiter);
        when(redisRateLimiter.tryConsume(startsWith("ratelimit:auth:ip:"), anyLong(), any())).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(429, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldLockTheAdminAccountAfterFiveWrongPasswordsWhateverTheIp() {
        AdminLoginAttemptService attempts = new AdminLoginAttemptService(redisTemplate);

        for (int i = 0; i < 4; i++) {
            attempts.recordFailure("oficial");
        }
        assertDoesNotThrow(() -> attempts.checkBlocked("oficial"));
        attempts.recordFailure("OFICIAL ");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> attempts.checkBlocked("oficial"));
        assertEquals("ADMIN_LOCKED", ex.getCode());
        assertEquals(Duration.ofMinutes(15), ttls.get("admin-login:blocked:oficial"));
        assertDoesNotThrow(() -> attempts.checkBlocked("otro-admin"));
    }

    @Test
    void shouldForgetFailedAttemptsAfterASuccessfulLogin() {
        AdminLoginAttemptService attempts = new AdminLoginAttemptService(redisTemplate);
        for (int i = 0; i < 4; i++) {
            attempts.recordFailure("oficial");
        }

        attempts.resetAttempts("oficial");
        attempts.recordFailure("oficial");

        assertDoesNotThrow(() -> attempts.checkBlocked("oficial"));
        verify(redisTemplate, atLeastOnce()).expire(eq("admin-login:attempts:oficial"), eq(Duration.ofHours(1)));
    }
}
