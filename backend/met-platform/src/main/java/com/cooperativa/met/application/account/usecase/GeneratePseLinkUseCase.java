package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.GeneratePseLinkRequest;
import com.cooperativa.met.application.account.dto.GeneratePseLinkResponse;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    private final CoreAccountRepositoryPort accountRepository;

    @Value("${met.wompi.public-key}")
    private String wompiPublicKey;

    @Value("${met.wompi.integrity-secret}")
    private String wompiIntegritySecret;

    @Value("${met.wompi.checkout-url:https://checkout.wompi.co/p/}")
    private String wompiCheckoutUrl;

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

        // Wompi requiere el monto en centavos (COP no tiene centavos, pero la API usa enteros)
        // Para COP: monto * 100 (Wompi trata todos los montos como la unidad mínima)
        long amountInCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        // Referencia única por transacción — incluye userId para el webhook
        String reference = "MET-" + userId.toString().replace("-", "").substring(0, 12).toUpperCase()
                + "-" + System.currentTimeMillis();

        // Firma de integridad SHA-256: reference + amountInCents + currency + integritySecret
        String currency = "COP";
        String integritySignature = generateIntegritySignature(reference, amountInCents, currency);

        // URL del checkout de Wompi con todos los parámetros
        String paymentUrl = wompiCheckoutUrl
                + "?public-key=" + wompiPublicKey
                + "&currency=" + currency
                + "&amount-in-cents=" + amountInCents
                + "&reference=" + reference
                + "&signature:integrity=" + integritySignature
                + "&redirect-url=" + buildRedirectUrl(userId, request.getReturnUrl());

        log.info("Generated Wompi checkout link for user {} with reference {}", userId, reference);

        return GeneratePseLinkResponse.builder()
                .paymentUrl(paymentUrl)
                .transactionId(reference)
                .build();
    }

    /**
     * Calcula la firma de integridad SHA-256 requerida por Wompi.
     * Formato: SHA256(reference + amountInCents + currency + integritySecret)
     */
    private String generateIntegritySignature(String reference, long amountInCents, String currency) {
        try {
            String data = reference + amountInCents + currency + wompiIntegritySecret;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating integrity signature", e);
            throw new BusinessRuleException("DEP_ERR_04", "Error generando la firma de pago");
        }
    }

    /**
     * Construye la URL de retorno a la que Wompi redirigirá al usuario
     * luego de completar el pago.
     */
    private String buildRedirectUrl(UUID userId, String returnUrl) {
        if (returnUrl != null && !returnUrl.isBlank()) {
            return returnUrl;
        }
        // URL de retorno por defecto: app móvil deep link
        return "metapp://deposit/success?userId=" + userId;
    }
}
