package com.cooperativa.met.infrastructure.web.webhook;

import com.cooperativa.met.application.account.usecase.ProcessWebhookUseCase;
import com.cooperativa.met.infrastructure.persistence.webhook.repository.ProcessedWebhookJpaRepository;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
    private static final String REFERENCIA = "MET-" + USER_ID + "-1735000000000";
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
    void unEventoAprobadoDeSandboxAcreditaElDeposito() {
        when(processedWebhookRepository.existsById("tx-42")).thenReturn(false);

        ResponseEntity<Void> response = controller.handlePaymentWebhook(
                null, null, event("APPROVED", 5_000_000L, APPROVED_CHECKSUM));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processWebhookUseCase).execute("tx-42", "WOMPI", USER_ID, new BigDecimal("50000"), "WOMPI_PSE");
    }

    @Test
    void aceptaElChecksumEnviadoEnLaCabecera() {
        when(processedWebhookRepository.existsById("tx-42")).thenReturn(false);

        ResponseEntity<Void> response = controller.handlePaymentWebhook(
                APPROVED_CHECKSUM.toLowerCase(), null, event("APPROVED", 5_000_000L, null));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processWebhookUseCase).execute(eq("tx-42"), eq("WOMPI"), eq(USER_ID), any(), anyString());
    }

    @Test
    void rechazaUnMontoAlterado() {
        ResponseEntity<Void> response = controller.handlePaymentWebhook(
                null, null, event("APPROVED", 900_000_000L, APPROVED_CHECKSUM));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void rechazaUnEventoFirmadoConOtroSecreto() {
        ReflectionTestUtils.setField(controller, "wompiWebhookSecret", "prod_events_other");

        ResponseEntity<Void> response = controller.handlePaymentWebhook(
                null, null, event("APPROVED", 5_000_000L, APPROVED_CHECKSUM));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void confirmaUnaTransaccionRechazadaSinAcreditar() {
        ResponseEntity<Void> response = controller.handlePaymentWebhook(
                null, null, event("DECLINED", 5_000_000L, DECLINED_CHECKSUM));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void rechazaUnEventoSinFirma() {
        ResponseEntity<Void> response = controller.handlePaymentWebhook(
                null, null, event("APPROVED", 5_000_000L, null));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void noAcreditaDosVecesUnaTransaccionYaProcesada() {
        when(processedWebhookRepository.existsById("tx-42")).thenReturn(true);

        ResponseEntity<Void> response = controller.handlePaymentWebhook(
                null, null, event("APPROVED", 5_000_000L, APPROVED_CHECKSUM));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void aceptaLaFirmaAntiguaQueSeUsaEnLasPruebasManuales() throws Exception {
        when(processedWebhookRepository.existsById("tx-42")).thenReturn(false);
        String aviso = event("APPROVED", 5_000_000L, null);

        ResponseEntity<Void> response = controller.handlePaymentWebhook(null, firmaAntigua(aviso), aviso);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processWebhookUseCase).execute("tx-42", "WOMPI", USER_ID, new BigDecimal("50000"), "WOMPI_PSE");
    }

    @Test
    void rechazaUnaFirmaAntiguaQueNoCorrespondeAlAviso() throws Exception {
        String aviso = event("APPROVED", 5_000_000L, null);

        ResponseEntity<Void> response = controller.handlePaymentWebhook(null, firmaAntigua(aviso + " "), aviso);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    // La firma de Wompi cubre el id, el estado y el monto (signature.properties) más el timestamp: por eso el
    // mismo checksum sirve para variar el tipo de evento, la moneda o la referencia en los casos siguientes.

    @Test
    void ignoraLosEventosDeWompiQueNoSonDeTransacciones() {
        ResponseEntity<Void> response = controller.handlePaymentWebhook(null, null,
                event("nequi_token.updated", "APPROVED", 5_000_000L, "COP", REFERENCIA, APPROVED_CHECKSUM));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void noAcreditaUnPagoEnUnaMonedaDistintaAPesos() {
        ResponseEntity<Void> response = controller.handlePaymentWebhook(null, null,
                event("transaction.updated", "APPROVED", 5_000_000L, "USD", REFERENCIA, APPROVED_CHECKSUM));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PEDIDO-12345", "MET-esto-no-es-un-uuid-1735000000000"})
    void noAcreditaUnPagoCuyaReferenciaNoIdentificaAUnSocio(String referencia) {
        when(processedWebhookRepository.existsById("tx-42")).thenReturn(false);

        ResponseEntity<Void> response = controller.handlePaymentWebhook(null, null,
                event("transaction.updated", "APPROVED", 5_000_000L, "COP", referencia, APPROVED_CHECKSUM));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void rechazaUnEventoQueNoDiceQuePropiedadesFirmo() {
        String sinPropiedades = event("APPROVED", 5_000_000L, APPROVED_CHECKSUM).replace(
                "\"properties\": [\"transaction.id\", \"transaction.status\", \"transaction.amount_in_cents\"],", "");

        ResponseEntity<Void> response = controller.handlePaymentWebhook(null, null, sinPropiedades);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void respondeErrorParaQueWompiReintenteSiNoSePuedeAcreditar() {
        when(processedWebhookRepository.existsById("tx-42")).thenReturn(false);
        doThrow(new IllegalStateException("La base de datos no responde"))
                .when(processWebhookUseCase).execute(any(), any(), any(), any(), any());

        ResponseEntity<Void> response = controller.handlePaymentWebhook(
                null, null, event("APPROVED", 5_000_000L, APPROVED_CHECKSUM));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void rechazaUnCuerpoQueNoEsJsonSinTratarloComoFallaDelServidor() {
        ResponseEntity<Void> response = controller.handlePaymentWebhook(null, null, "esto no es un aviso de Wompi");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void rechazaUnAvisoFirmadoQueNoTraeEventoNiDatos() throws Exception {
        String aviso = "{\"environment\":\"test\",\"timestamp\":1735000000}";

        ResponseEntity<Void> response = controller.handlePaymentWebhook(null, firmaAntigua(aviso), aviso);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(processWebhookUseCase, never()).execute(any(), any(), any(), any(), any());
    }

    /** Firma antigua (cabecera X-Signature): HMAC-SHA256 del cuerpo con el secreto de eventos, en Base64. */
    private static String firmaAntigua(String cuerpo) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(EVENTS_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(hmac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8)));
    }

    private static String event(String status, long amountInCents, String checksum) {
        return event("transaction.updated", status, amountInCents, "COP", REFERENCIA, checksum);
    }

    private static String event(String eventName, String status, long amountInCents, String currency,
                                String reference, String checksum) {
        String signature = checksum == null
                ? "\"properties\": [\"transaction.id\", \"transaction.status\", \"transaction.amount_in_cents\"]"
                : "\"properties\": [\"transaction.id\", \"transaction.status\", \"transaction.amount_in_cents\"],"
                        + " \"checksum\": \"" + checksum + "\"";
        return """
                {
                  "event": "%s",
                  "data": {
                    "transaction": {
                      "id": "tx-42",
                      "amount_in_cents": %d,
                      "reference": "%s",
                      "customer_email": "socio@example.com",
                      "currency": "%s",
                      "payment_method_type": "PSE",
                      "status": "%s"
                    }
                  },
                  "environment": "test",
                  "signature": { %s },
                  "timestamp": 1735000000,
                  "sent_at": "2024-12-24T00:26:40.000Z"
                }
                """.formatted(eventName, amountInCents, reference, currency, status, signature);
    }
}
