package com.cooperativa.fintech.infrastructure.cache;

import com.cooperativa.fintech.domain.savings.port.SavingsBalanceCachePort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class InMemorySavingsBalanceCacheAdapter implements SavingsBalanceCachePort {

    private final Map<UUID, BigDecimal> cache = new ConcurrentHashMap<>();

    @Override
    public void cacheBalance(UUID accountId, BigDecimal balance) {
        cache.put(accountId, balance);
    }

    @Override
    public Optional<BigDecimal> getCachedBalance(UUID accountId) {
        return Optional.ofNullable(cache.get(accountId));
    }

    @Override
    public void invalidate(UUID accountId) {
        cache.remove(accountId);
    }
}
