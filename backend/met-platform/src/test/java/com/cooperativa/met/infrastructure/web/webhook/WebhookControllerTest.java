package com.cooperativa.met.infrastructure.web.webhook;

import com.cooperativa.met.application.account.usecase.ProcessWebhookUseCase;
import com.cooperativa.met.infrastructure.persistence.webhook.repository.ProcessedWebhookJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Eventos con el formato real de Wompi (docs.wompi.co/docs/colombia/eventos/).
 * Los checksums se calcularon aparte con sha256sum, p. ej.:
 * {@code printf '%s' "tx-42APPROVED50000001735000000test_events_secret" | sha256sum}
 */
@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    private static final String EVENTS_SECRET = "test_events_secret";
    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String APPROVED_CHECKSUM = "7D0A0C6F1AB5162D0536B696578C4AB11B018854A2438AE2E8E29F22D8E14896";
    private static final String DECLINED_CHECKSUM = "972E92DBBAD8C1F78CB9197B7F4744ADDFA8D07BC105983D9F1C921983CE75FF";

    @Mock
    private ProcessWebhookUseCase processWebhookUseCase;

    @Mock
    private ProcessedWebhookJpaRepository processedWebhookRepository;

    private WebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new WebhookController(processWebhookUseCase, processedWebhookRepository, new ObjectMapper());
        ReflectionTestUtils.setField(controller, "wompiWebhookSecret", EVENTS_SECRET);
    }

    @Test
    void approvedSandboxEventCreditsTheDeposit() {
        when(processedWebhookRepository.existsById("tx-42")).thenReturn(false);

        ResponseEntity<Void> response = controller.handlePaymentWebhook(
                null, null, event("APPROVED", 5_000_000L, APPROVED_CHECKSUM));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processWebhookUseCase).execute("tx-42", "WOMPI", USER_ID, new BigDecimal("50000"), "WOMPI_PSE");
    }

    @Test
    void checksumFromHeaderIsAccepted() {
        when(processedWebhookRepository.existsById("tx-42")).thenReturn(false);

        ResponseEntity<Void> response = controller.handlePaymentWebhook(
                APPROVED_CHECKSUM.toLowerCase(), null, event("APPROVED", 5_000_000L, null));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processWebhookUseCase).execute(eq("tx-42"), eq("WOMPI"), eq(USER_ID), any(), anyString());
    }

    @Test
    void tamperedAmountIsRejected() {
        ResponseEntity<Void> response = controller.handlePaymentWebhook(
                null, null, event("APPROVED", 900_000_000L, APPROVED_CHECKSUM));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void eventSignedWithAnotherSecretIsRejected() {
        ReflectionTestUtils.setField(controller, "wompiWebhookSecret", "prod_events_other");

        ResponseEntity<Void> response = controller.handlePaymentWebhook(
                null, null, event("APPROVED", 5_000_000L, APPROVED_CHECKSUM));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void declinedTransactionIsAcknowledgedWithoutCrediting() {
        ResponseEntity<Void> response = controller.handlePaymentWebhook(
                null, null, event("DECLINED", 5_000_000L, DECLINED_CHECKSUM));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void unsignedEventIsRejected() {
        ResponseEntity<Void> response = controller.handlePaymentWebhook(
                null, null, event("APPROVED", 5_000_000L, null));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void alreadyProcessedTransactionIsNotCreditedTwice() {
        when(processedWebhookRepository.existsById("tx-42")).thenReturn(true);

        ResponseEntity<Void> response = controller.handlePaymentWebhook(
                null, null, event("APPROVED", 5_000_000L, APPROVED_CHECKSUM));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    private static String event(String status, long amountInCents, String checksum) {
        String signature = checksum == null
                ? "\"properties\": [\"transaction.id\", \"transaction.status\", \"transaction.amount_in_cents\"]"
                : "\"properties\": [\"transaction.id\", \"transaction.status\", \"transaction.amount_in_cents\"],"
                        + " \"checksum\": \"" + checksum + "\"";
        return """
                {
                  "event": "transaction.updated",
                  "data": {
                    "transaction": {
                      "id": "tx-42",
                      "amount_in_cents": %d,
                      "reference": "MET-%s-1735000000000",
                      "customer_email": "socio@example.com",
                      "currency": "COP",
                      "payment_method_type": "PSE",
                      "status": "%s"
                    }
                  },
                  "environment": "test",
                  "signature": { %s },
                  "timestamp": 1735000000,
                  "sent_at": "2024-12-24T00:26:40.000Z"
                }
                """.formatted(amountInCents, USER_ID, status, signature);
    }
}
