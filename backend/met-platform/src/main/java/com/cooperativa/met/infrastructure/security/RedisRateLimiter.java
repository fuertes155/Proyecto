package com.cooperativa.met.infrastructure.security;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;

    public boolean tryConsume(String key, long limit, Duration ttl) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        Long current;
        try {
            current = ops.increment(key);
        } catch (Exception ignored) {
            // fallback: si el incremento falla, permitimos por defecto
            return true;
        }

        if (current == null) {
            return true;
        }

        // set TTL solo cuando se crea la clave (cuando pasa a 1)
        if (current == 1L) {
            redisTemplate.expire(key, ttl);
        }

        return current <= limit;
    }
}
