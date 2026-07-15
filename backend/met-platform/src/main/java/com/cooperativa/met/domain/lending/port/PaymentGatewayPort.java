package com.cooperativa.met.domain.lending.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGatewayPort {
    boolean chargeTokenizedCard(UUID userId, String token, BigDecimal amount, String reference);
}
