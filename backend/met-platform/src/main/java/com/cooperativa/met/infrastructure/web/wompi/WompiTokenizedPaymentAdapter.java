package com.cooperativa.met.infrastructure.web.wompi;

import com.cooperativa.met.domain.lending.port.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Slf4j
public class WompiTokenizedPaymentAdapter implements PaymentGatewayPort {

    @Override
    public boolean chargeTokenizedCard(UUID userId, String token, BigDecimal amount, String reference) {
        log.info("[WOMPI MOCK] Intentando cargo silencioso a tarjeta tokenizada.");
        log.info(" - Usuario: {}", userId);
        log.info(" - Token: {}", token);
        log.info(" - Monto: {}", amount);
        log.info(" - Ref: {}", reference);

        // Simulamos que el cargo con el token a Wompi fue exitoso
        log.info("[WOMPI MOCK] ¡Cargo exitoso!");
        return true;
    }
}
