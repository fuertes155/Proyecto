package com.cooperativa.fintech.domain.savings.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface DebitSourcePort {

    boolean debit(UUID userId, BigDecimal amount, String reference);
}
