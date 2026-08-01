package com.cooperativa.met.infrastructure.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cálculo de la firma HMAC-SHA256 usada por {@link HmacSignatureFilter} para
 * validar la integridad de las peticiones firmadas por la app móvil.
 * Extraído a una clase propia para que los tests de integración puedan
 * firmar peticiones reales en vez de depender de un bypass.
 */
public final class HmacSigner {

    private static final String HMAC_ALGO = "HmacSHA256";

    private HmacSigner() {
    }

    public static String sign(String method, String path, String timestamp, String body, String secret) {
        try {
            String dataToSign = method + path + timestamp + body;
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] hmacBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC", e);
        }
    }
}
