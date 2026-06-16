package com.cooperativa.met.infrastructure.cache;

import com.cooperativa.met.domain.solidarity.port.SolidarityPoolCachePort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class InMemorySolidarityPoolCacheAdapter implements SolidarityPoolCachePort {

    private final Map<UUID, BigDecimal> cache = new ConcurrentHashMap<>();

    @Override
    public void cacheBalance(UUID groupId, BigDecimal balance) {
        cache.put(groupId, balance);
    }

    @Override
    public Optional<BigDecimal> getCachedBalance(UUID groupId) {
        return Optional.ofNullable(cache.get(groupId));
    }

    @Override
    public void invalidate(UUID groupId) {
        cache.remove(groupId);
    }
}
