package com.cooperativa.fintech.infrastructure.cache;

import com.cooperativa.fintech.domain.savings.port.SavingsBalanceCachePort;
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
public class RedisSavingsBalanceCacheAdapter implements SavingsBalanceCachePort {

    private static final String KEY_PREFIX = "savings:balance:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void cacheBalance(UUID accountId, BigDecimal balance) {
        redisTemplate.opsForValue().set(key(accountId), balance.toPlainString(), TTL);
    }

    @Override
    public Optional<BigDecimal> getCachedBalance(UUID accountId) {
        String value = redisTemplate.opsForValue().get(key(accountId));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(value));
    }

    @Override
    public void invalidate(UUID accountId) {
        redisTemplate.delete(key(accountId));
    }

    private String key(UUID accountId) {
        return KEY_PREFIX + accountId;
    }
}
