package com.cooperativa.met.infrastructure.adapter.wompi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WompiSignaturesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void integrityIsSha256OfReferenceAmountCurrencyAndSecret() {
        // printf '%s' "MET-ref-1735000000000""5000000""COP""test_integrity_abc123" | sha256sum
        assertEquals("dc63dc1d2a8553642cdca5fbf663d36abfe42258d52ec534366201a7e6703e17",
                WompiSignatures.integrity("MET-ref-1735000000000", 5_000_000L, "COP", "test_integrity_abc123"));
    }

    @Test
    void eventChecksumConcatenatesSignedPropertiesTimestampAndSecret() throws Exception {
        JsonNode event = objectMapper.readTree("""
                {
                  "event": "transaction.updated",
                  "data": { "transaction": { "id": "tx-42", "status": "APPROVED", "amount_in_cents": 5000000 } },
                  "signature": {
                    "properties": ["transaction.id", "transaction.status", "transaction.amount_in_cents"],
                    "checksum": "ignored-here"
                  },
                  "timestamp": 1735000000
                }
                """);

        // printf '%s' "tx-42APPROVED5000000""1735000000""test_events_secret" | sha256sum
        assertEquals("7d0a0c6f1ab5162d0536b696578c4ab11b018854a2438ae2e8e29f22d8e14896",
                WompiSignatures.eventChecksum(event, "test_events_secret"));
    }

    @Test
    void eventChecksumIsNullWhenASignedPropertyIsMissing() throws Exception {
        JsonNode event = objectMapper.readTree("""
                {
                  "data": { "transaction": { "id": "tx-42" } },
                  "signature": { "properties": ["transaction.id", "transaction.status"] },
                  "timestamp": 1735000000
                }
                """);

        assertNull(WompiSignatures.eventChecksum(event, "test_events_secret"));
    }

    @Test
    void eventChecksumIsNullWithoutTimestamp() throws Exception {
        JsonNode event = objectMapper.readTree("""
                { "data": { "transaction": { "id": "tx-42" } }, "signature": { "properties": ["transaction.id"] } }
                """);

        assertNull(WompiSignatures.eventChecksum(event, "test_events_secret"));
    }

    @Test
    void checksumComparisonIgnoresCase() {
        assertTrue(WompiSignatures.checksumMatches("7d0a0c6f", "7D0A0C6F"));
        assertFalse(WompiSignatures.checksumMatches("7d0a0c6f", "7d0a0c6e"));
        assertFalse(WompiSignatures.checksumMatches("7d0a0c6f", null));
    }
}
