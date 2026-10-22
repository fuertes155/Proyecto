package com.cooperativa.met.infrastructure.security;

import com.cooperativa.met.infrastructure.cache.RedisSavingsBalanceCacheAdapter;
import com.cooperativa.met.infrastructure.cache.RedisSolidarityPoolCacheAdapter;
import com.cooperativa.met.infrastructure.config.TurnstileProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Captcha de Cloudflare Turnstile, lista negra de tokens del logout y cachés de saldos en Redis. */
@ExtendWith(MockitoExtension.class)
class TurnstileAndTokenBlacklistTest {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @Test
    void shouldAcceptOnlyTokensThatCloudflareConfirms() {
        TurnstileValidationService turnstile = turnstile(true);
        MockRestServiceServer server = server(turnstile);
        server.expect(requestTo(VERIFY_URL))
                .andExpect(content().formDataContains(java.util.Map.of("secret", "cf-secret", "response", "tok-ok", "remoteip", "190.85.10.4")))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}", MediaType.APPLICATION_JSON));

        assertTrue(turnstile.validateCaptcha("tok-ok", "190.85.10.4"));
        assertFalse(turnstile.validateCaptcha("tok-falso", null));
    }

    @Test
    void shouldFailClosedWhenCloudflareIsDownOrTheTokenIsMissing() {
        TurnstileValidationService turnstile = turnstile(true);
        server(turnstile).expect(requestTo(VERIFY_URL)).andRespond(withServerError());

        assertFalse(turnstile.validateCaptcha("tok", "1.1.1.1"), "un servicio financiero no deja pasar si no puede verificar");
        assertFalse(turnstile.validateCaptcha("  ", "1.1.1.1"));
    }

    @Test
    void shouldSkipTheCaptchaWhenItIsDisabled() {
        assertTrue(turnstile(false).validateCaptcha(null, null));
    }

    @Test
    void shouldBlacklistTokensOnlyForTheirRemainingLifetime() {
        TokenBlacklistService blacklist = new TokenBlacklistService(stringRedisTemplate);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(stringRedisTemplate.hasKey("jwt:blacklist:jwt-1")).thenReturn(true);

        blacklist.revoke("jwt-1", Duration.ofMinutes(30));
        blacklist.revoke("jwt-vencido", Duration.ZERO);

        verify(valueOps).set("jwt:blacklist:jwt-1", "1", Duration.ofMinutes(30));
        assertTrue(blacklist.isRevoked("jwt-1"));
        assertFalse(blacklist.isRevoked("jwt-2"));
    }

    @Test
    void shouldKeepWorkingWhenRedisIsDown() {
        TokenBlacklistService blacklist = new TokenBlacklistService(stringRedisTemplate);
        when(stringRedisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));
        when(stringRedisTemplate.hasKey(any())).thenThrow(new RedisConnectionFailureException("down"));

        assertDoesNotThrow(() -> blacklist.revoke("jwt-1", Duration.ofMinutes(30)));
        // Comportamiento actual (fail-open): con Redis caído, un token cerrado con logout vuelve a servir.
        assertFalse(blacklist.isRevoked("jwt-1"));
    }

    @Test
    void shouldCacheBalancesForFiveMinutes() {
        UUID id = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("savings:balance:" + id)).thenReturn("1250000.50");
        when(valueOps.get("solidarity:pool:" + id)).thenReturn(null);
        RedisSavingsBalanceCacheAdapter savings = new RedisSavingsBalanceCacheAdapter(redisTemplate);
        RedisSolidarityPoolCacheAdapter pool = new RedisSolidarityPoolCacheAdapter(redisTemplate);

        savings.cacheBalance(id, new BigDecimal("1250000.50"));
        pool.cacheBalance(id, new BigDecimal("500000"));
        savings.invalidate(id);
        pool.invalidate(id);

        verify(valueOps).set("savings:balance:" + id, "1250000.50", Duration.ofMinutes(5));
        verify(valueOps).set("solidarity:pool:" + id, "500000", Duration.ofMinutes(5));
        verify(redisTemplate).delete("savings:balance:" + id);
        verify(redisTemplate).delete("solidarity:pool:" + id);
        assertEquals(Optional.of(new BigDecimal("1250000.50")), savings.getCachedBalance(id));
        assertEquals(Optional.empty(), pool.getCachedBalance(id));
    }

    private TurnstileValidationService turnstile(boolean enabled) {
        TurnstileProperties properties = new TurnstileProperties();
        properties.setEnabled(enabled);
        properties.setSecretKey("cf-secret");
        return new TurnstileValidationService(properties);
    }

    private static MockRestServiceServer server(TurnstileValidationService turnstile) {
        return MockRestServiceServer.bindTo((RestTemplate) ReflectionTestUtils.getField(turnstile, "restTemplate")).build();
    }
}
