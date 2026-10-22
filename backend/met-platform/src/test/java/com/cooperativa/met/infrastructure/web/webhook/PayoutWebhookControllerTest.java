package com.cooperativa.met.infrastructure.web.webhook;

import com.cooperativa.met.application.bank.usecase.ProcessPayoutWebhookUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Webhook de "Pagos a Terceros": solo se procesa lo firmado con el secreto compartido. */
@ExtendWith(MockitoExtension.class)
class PayoutWebhookControllerTest {

    private static final String SECRET = "test-payouts-webhook-secret";
    private static final String TIMESTAMP = "1792629663";

    @Mock
    private ProcessPayoutWebhookUseCase processPayoutWebhookUseCase;

    private PayoutWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new PayoutWebhookController(processPayoutWebhookUseCase, new ObjectMapper());
        ReflectionTestUtils.setField(controller, "payoutWebhookSecret", SECRET);
    }

    @Test
    void shouldSettlePayoutOnSignedApprovedEvent() throws Exception {
        String payload = payload("APPROVED", null);

        ResponseEntity<Void> response = controller.handlePayoutWebhook(signature(payload), payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processPayoutWebhookUseCase).execute("PAYOUT-123", true, null, null);
    }

    @Test
    void shouldReverseThePayoutWithTheProvidersReasonOnFailedEvent() throws Exception {
        String payload = payload("declined", "{\"code\":\"ACCOUNT_CLOSED\",\"message\":\"Cuenta cerrada\"}");

        ResponseEntity<Void> response = controller.handlePayoutWebhook(signature(payload), payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processPayoutWebhookUseCase).execute("PAYOUT-123", false, "ACCOUNT_CLOSED", "Cuenta cerrada");
    }

    @Test
    void shouldIgnoreIntermediateStates() throws Exception {
        String payload = payload("PENDING", null);

        assertEquals(HttpStatus.OK, controller.handlePayoutWebhook(signature(payload), payload).getStatusCode());

        verifyNoInteractions(processPayoutWebhookUseCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "v1=abc", "t=1792629663", "t=1792629663,v1=0000000000000000000000000000000000000000000000000000000000000000"})
    void shouldRejectMissingMalformedOrForgedSignatures(String header) {
        String payload = payload("APPROVED", null);

        assertEquals(HttpStatus.UNAUTHORIZED, controller.handlePayoutWebhook(header, payload).getStatusCode());

        verifyNoInteractions(processPayoutWebhookUseCase);
    }

    @Test
    void shouldRejectAnUnsignedRequest() {
        assertEquals(HttpStatus.UNAUTHORIZED, controller.handlePayoutWebhook(null, payload("APPROVED", null)).getStatusCode());
    }

    @Test
    void shouldRejectAPayloadChangedAfterSigning() throws Exception {
        String signedAsFailed = payload("FAILED", null);
        String tamperedToApproved = payload("APPROVED", null);

        assertEquals(HttpStatus.UNAUTHORIZED,
                controller.handlePayoutWebhook(signature(signedAsFailed), tamperedToApproved).getStatusCode());
        verifyNoInteractions(processPayoutWebhookUseCase);
    }

    @Test
    void shouldAnswerBadRequestWhenTheEventHasNoTransaction() throws Exception {
        String payload = "{\"event\":\"payout.updated\",\"data\":{}}";

        assertEquals(HttpStatus.BAD_REQUEST, controller.handlePayoutWebhook(signature(payload), payload).getStatusCode());
    }

    @Test
    void shouldAnswerServerErrorSoTheProviderRetriesWhenProcessingFails() throws Exception {
        String payload = payload("APPROVED", null);
        doThrow(new IllegalStateException("BD caída")).when(processPayoutWebhookUseCase).execute(any(), anyBoolean(), any(), any());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.handlePayoutWebhook(signature(payload), payload).getStatusCode());
    }

    private static String payload(String status, String failureReason) {
        return "{\"event\":\"payout.updated\",\"data\":{\"transaction\":{\"id\":\"w-1\",\"reference\":\"PAYOUT-123\",\"status\":\""
                + status + "\"" + (failureReason != null ? ",\"failureReason\":" + failureReason : "") + "}},\"sent_at\":\"2026-10-21\"}";
    }

    private static String signature(String payload) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest((TIMESTAMP + "." + payload + SECRET).getBytes(StandardCharsets.UTF_8));
        return "t=" + TIMESTAMP + ",v1=" + HexFormat.of().formatHex(hash);
    }
}
