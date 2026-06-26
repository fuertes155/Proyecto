package com.cooperativa.met.infrastructure.persistence.investment.adapter;

import com.cooperativa.met.domain.investment.port.InvestmentCreditPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Implementación stub del puerto de crédito de inversiones.
 *
 * En producción, este adaptador integraría con el sistema de saldo principal
 * (cuenta cooperativa, pasarela de pagos, etc.) para acreditar fondos al usuario.
 * Por ahora simula el crédito exitoso, al igual que StubDebitSourceAdapter.
 */
@Slf4j
@Component
public class StubInvestmentCreditAdapter implements InvestmentCreditPort {

    @Override
    public boolean credit(UUID userId, BigDecimal amount, String reference) {
        // Integración futura con cuenta de saldo principal / pasarela de pagos
        log.debug("Crédito simulado de inversión: userId={}, amount={}, ref={}", userId, amount, reference);
        return true;
    }
}
