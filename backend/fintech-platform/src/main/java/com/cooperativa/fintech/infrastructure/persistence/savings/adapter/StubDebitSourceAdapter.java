package com.cooperativa.fintech.infrastructure.persistence.savings.adapter;

import com.cooperativa.fintech.domain.savings.port.DebitSourcePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class StubDebitSourceAdapter implements DebitSourcePort {

    @Override
    public boolean debit(UUID userId, BigDecimal amount, String reference) {
        // Integración futura con cuenta de ahorros principal / pasarela de pagos
        log.debug("Débito simulado: userId={}, amount={}, ref={}", userId, amount, reference);
        return true;
    }
}
