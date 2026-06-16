package com.cooperativa.met.domain.savings.port;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface SavingsBalanceCachePort {

    void cacheBalance(UUID accountId, BigDecimal balance);

    Optional<BigDecimal> getCachedBalance(UUID accountId);

    void invalidate(UUID accountId);
}
