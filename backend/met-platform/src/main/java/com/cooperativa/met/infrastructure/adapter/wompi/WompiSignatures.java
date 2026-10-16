package com.cooperativa.met.infrastructure.adapter.wompi;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Las dos firmas SHA-256 que usa Wompi, en un solo lugar para que el checkout
 * hosteado, la API de transacciones y el webhook no puedan divergir.
 *
 * <ul>
 *   <li><b>Integridad</b> (docs.wompi.co → Widget &amp; Checkout Web → "Genera una
 *       firma de integridad"): {@code SHA256(reference + amountInCents + currency + integritySecret)}.
 *       Obligatoria en el checkout hosteado y en {@code POST /transactions}.</li>
 *   <li><b>Checksum de eventos</b> (docs.wompi.co → Eventos): concatenar los valores de
 *       {@code signature.properties} (rutas dentro de {@code data}), luego {@code timestamp}
 *       y al final el secreto de eventos ({@code test_events_…} / {@code prod_events_…}).</li>
 * </ul>
 */
public final class WompiSignatures {

    private WompiSignatures() {
    }

    public static String integrity(String reference, long amountInCents, String currency, String integritySecret) {
        return sha256Hex(reference + amountInCents + currency + integritySecret);
    }

    /**
     * Calcula el checksum esperado de un evento de Wompi a partir del JSON crudo.
     *
     * @return el checksum en hexadecimal, o {@code null} si el evento no trae
     *         {@code signature.properties}/{@code timestamp} o alguna propiedad
     *         referenciada no existe en {@code data}.
     */
    public static String eventChecksum(JsonNode event, String eventsSecret) {
        JsonNode properties = event.path("signature").path("properties");
        JsonNode timestamp = event.get("timestamp");
        JsonNode data = event.get("data");
        if (!properties.isArray() || properties.isEmpty() || timestamp == null || data == null) {
            return null;
        }

        StringBuilder concatenated = new StringBuilder();
        for (JsonNode property : properties) {
            JsonNode value = data;
            for (String segment : property.asText().split("\\.")) {
                value = value.get(segment);
                if (value == null) {
                    return null;
                }
            }
            if (value.isNull() || value.isContainerNode()) {
                return null;
            }
            concatenated.append(value.asText());
        }
        concatenated.append(timestamp.asText()).append(eventsSecret);
        return sha256Hex(concatenated.toString());
    }

    /** Compara dos checksums hex en tiempo constante, sin importar mayúsculas (Wompi los envía en mayúscula). */
    public static boolean checksumMatches(String expectedHex, String receivedHex) {
        if (expectedHex == null || receivedHex == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedHex.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                receivedHex.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    static String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }
}
