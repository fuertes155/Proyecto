package com.cooperativa.fintech.domain.solidarity.port;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface SolidarityPoolCachePort {

    void cacheBalance(UUID groupId, BigDecimal balance);

    Optional<BigDecimal> getCachedBalance(UUID groupId);

    void invalidate(UUID groupId);
}
