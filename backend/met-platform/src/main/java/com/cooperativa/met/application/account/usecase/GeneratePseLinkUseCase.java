package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.GeneratePseLinkRequest;
import com.cooperativa.met.application.account.dto.GeneratePseLinkResponse;
import com.cooperativa.met.application.account.service.PaymentReturnUrlResolver;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.infrastructure.adapter.wompi.WompiSignatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Genera un link de pago de Wompi Checkout para que el usuario complete
 * el depósito a través de cualquier método soportado por Wompi
 * (PSE, Nequi, Bre-B, tarjetas de crédito, etc.).
 *
 * La firma de integridad (SHA-256) garantiza que el monto y la referencia
 * no puedan ser manipulados por el cliente una vez generada la URL.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeneratePseLinkUseCase {

    private static final String CURRENCY = "COP";

    private final CoreAccountRepositoryPort accountRepository;
    private final PaymentReturnUrlResolver returnUrlResolver;

    @Value("${met.wompi.public-key}")
    private String wompiPublicKey;

    @Value("${met.wompi.integrity-secret}")
    private String wompiIntegritySecret;

    @Value("${met.wompi.checkout-url:https://checkout.wompi.co/p/}")
    private String wompiCheckoutUrl;

    /** "wompi" (sandbox o producción, según llaves y URL) o "mock" (pasarela simulada local). */
    @Value("${met.payments.gateway:wompi}")
    private String paymentsGateway;

    /**
     * Con {@code met.payments.gateway=mock} y esta URL definida (dev/demo), los depósitos
     * generan un link a la pasarela de pago SIMULADA ({@code /api/v1/mock-payment-gateway})
     * en vez del checkout real de Wompi. Permite probar el flujo completo de depósito
     * (incluida la distribución de capital) sin credenciales de Wompi.
     */
    @Value("${met.payments.mock-gateway-base-url:}")
    private String mockGatewayBaseUrl;

    /**
     * Genera la URL del checkout de Wompi con firma de integridad SHA-256.
     *
     * @param userId  ID del usuario autenticado
     * @param request monto y URL de retorno
     * @return URL firmada del checkout + ID de referencia
     */
    public GeneratePseLinkResponse execute(UUID userId, GeneratePseLinkRequest request) {
        if (request.getAmount() == null || request.getAmount().doubleValue() <= 0) {
            throw new BusinessRuleException("DEP_ERR_01", "El monto a recargar debe ser mayor a cero");
        }

        CoreAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessRuleException("DEP_ERR_02", "No tienes una cuenta activa para recargar"));

        if (!account.getStatus().name().equals("ACTIVE")) {
            throw new BusinessRuleException("DEP_ERR_03", "Tu cuenta no está activa");
        }

        // Wompi recibe los montos en centavos, también para COP: monto * 100
        long amountInCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        // Referencia única por transacción — incluye userId COMPLETO para el webhook
        String reference = "MET-" + userId.toString() + "-" + System.currentTimeMillis();

        // --- DEV/DEMO: pasarela de pago simulada ---
        if ("mock".equalsIgnoreCase(paymentsGateway) && mockGatewayBaseUrl != null && !mockGatewayBaseUrl.isBlank()) {
            String base = mockGatewayBaseUrl.endsWith("/")
                    ? mockGatewayBaseUrl.substring(0, mockGatewayBaseUrl.length() - 1)
                    : mockGatewayBaseUrl;
            String mockUrl = base + "/api/v1/mock-payment-gateway"
                    + "?transactionId=" + reference
                    + "&amount=" + request.getAmount().toPlainString()
                    + "&userId=" + userId;
            log.info("[DEV] Generated MOCK payment link for user {} with reference {}", userId, reference);
            return GeneratePseLinkResponse.builder()
                    .paymentUrl(mockUrl)
                    .transactionId(reference)
                    .build();
        }

        String integritySignature = WompiSignatures.integrity(reference, amountInCents, CURRENCY, wompiIntegritySecret);

        String paymentUrl = wompiCheckoutUrl
                + "?public-key=" + encode(wompiPublicKey)
                + "&currency=" + CURRENCY
                + "&amount-in-cents=" + amountInCents
                + "&reference=" + encode(reference)
                + "&signature:integrity=" + integritySignature
                + "&redirect-url=" + encode(returnUrlResolver.resolve(userId, request.getReturnUrl()));

        log.info("Generated Wompi checkout link for user {} with reference {}", userId, reference);

        return GeneratePseLinkResponse.builder()
                .paymentUrl(paymentUrl)
                .transactionId(reference)
                .build();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
