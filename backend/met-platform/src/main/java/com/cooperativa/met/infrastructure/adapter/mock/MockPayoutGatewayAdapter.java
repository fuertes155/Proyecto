package com.cooperativa.met.infrastructure.adapter.mock;

import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import com.cooperativa.met.domain.bank.port.PayoutGatewayBank;
import com.cooperativa.met.domain.bank.port.PayoutGatewayPort;
import com.cooperativa.met.domain.bank.port.PayoutGatewayRequest;
import com.cooperativa.met.domain.bank.port.PayoutGatewayResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementación DEV de {@link PayoutGatewayPort}. Sustituye a
 * {@code WompiPayoutAdapter} cuando el perfil activo es "dev": acepta todos los
 * payouts (retiros y micro-depósitos de verificación de cuenta) sin llamar a
 * Wompi. Permite probar el flujo completo de retiro a banco externo y de
 * verificación de cuenta bancaria en local.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class MockPayoutGatewayAdapter implements PayoutGatewayPort {

    private final BankRepositoryPort bankRepository;

    @Override
    public List<PayoutGatewayBank> fetchSupportedBanks() {
        return bankRepository.findAllActive().stream()
                .filter(b -> b.isSupportsPayout())
                .map(b -> new PayoutGatewayBank(b.getCode(), b.getName(), true))
                .toList();
    }

    @Override
    public PayoutGatewayResult initiatePayout(PayoutGatewayRequest request) {
        log.info("[DEV] Payout simulado ACEPTADO: reference={} monto={} banco={} cuenta=****{}",
                request.reference(), request.amount(), request.destinationWompiBankId(),
                lastFour(request.destinationAccountNumber()));
        return new PayoutGatewayResult("MOCK-RAIL-" + request.reference(),
                PayoutGatewayResult.PayoutGatewayStatus.ACCEPTED, null, null);
    }

    private static String lastFour(String s) {
        if (s == null || s.length() < 4) return s == null ? "" : s;
        return s.substring(s.length() - 4);
    }
}
