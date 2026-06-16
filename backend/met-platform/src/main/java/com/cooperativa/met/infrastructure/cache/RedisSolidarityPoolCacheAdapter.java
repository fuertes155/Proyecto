package com.cooperativa.met.infrastructure.cache;

import com.cooperativa.met.domain.solidarity.port.SolidarityPoolCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RedisSolidarityPoolCacheAdapter implements SolidarityPoolCachePort {

    private static final String KEY_PREFIX = "solidarity:pool:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void cacheBalance(UUID groupId, BigDecimal balance) {
        redisTemplate.opsForValue().set(key(groupId), balance.toPlainString(), TTL);
    }

    @Override
    public Optional<BigDecimal> getCachedBalance(UUID groupId) {
        String value = redisTemplate.opsForValue().get(key(groupId));
        return value == null ? Optional.empty() : Optional.of(new BigDecimal(value));
    }

    @Override
    public void invalidate(UUID groupId) {
        redisTemplate.delete(key(groupId));
    }

    private String key(UUID groupId) {
        return KEY_PREFIX + groupId;
    }
}
