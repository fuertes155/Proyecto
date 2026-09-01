package com.cooperativa.met.infrastructure.adapter.mock;

import com.cooperativa.met.domain.bank.port.PseFinancialInstitution;
import com.cooperativa.met.domain.bank.port.PseGatewayPort;
import com.cooperativa.met.domain.bank.port.PseTransactionRequest;
import com.cooperativa.met.domain.bank.port.PseTransactionResult;
import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementación DEV de {@link PseGatewayPort}. Sustituye a {@code WompiPseAdapter}
 * cuando el perfil activo es "dev": el catálogo de bancos sale de la BD local y la
 * transacción PSE redirige a la pasarela de pago SIMULADA
 * ({@code /api/v1/mock-payment-gateway}), que al confirmar dispara el webhook
 * {@code /api/v1/webhooks/mock-payment} y acredita el depósito.
 *
 * Requiere {@code met.payments.mock-gateway-base-url} configurado (ver .env).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class MockPseGatewayAdapter implements PseGatewayPort {

    private final BankRepositoryPort bankRepository;

    @Value("${met.payments.mock-gateway-base-url:}")
    private String mockGatewayBaseUrl;

    @Override
    public List<PseFinancialInstitution> fetchFinancialInstitutions() {
        return bankRepository.findAllActive().stream()
                .filter(b -> b.isSupportsPse())
                .map(b -> new PseFinancialInstitution(b.getCode(), b.getName()))
                .toList();
    }

    @Override
    public PseTransactionResult createPseTransaction(PseTransactionRequest request) {
        // La 'reference' viene como "MET-<userId>-<timestamp>" (ver CreateNativePseDepositUseCase).
        String userId = extractUserId(request.reference());
        String base = trimSlash(mockGatewayBaseUrl);
        String url = base + "/api/v1/mock-payment-gateway"
                + "?transactionId=" + request.reference()
                + "&amount=" + request.amount().toPlainString()
                + "&userId=" + userId;
        log.info("[DEV] PSE nativo simulado: reference={} banco={} -> {}",
                request.reference(), request.financialInstitutionCode(), url);
        return new PseTransactionResult(request.reference(), url, "PENDING");
    }

    private static String extractUserId(String reference) {
        if (reference == null) return "";
        String[] p = reference.split("-", 2);          // "MET" | "<uuid>-<ts>"
        if (p.length < 2) return "";
        String rest = p[1];
        int lastDash = rest.lastIndexOf('-');           // el UUID tiene guiones; el último separa el timestamp
        return lastDash > 0 ? rest.substring(0, lastDash) : rest;
    }

    private static String trimSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
