package com.cooperativa.met.infrastructure.web.webhook;

import com.cooperativa.met.TestRedisConfig;
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.TokenPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import com.cooperativa.met.infrastructure.security.HmacSigner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El depósito del sandbox de Wompi de punta a punta, con la aplicación real (seguridad, filtros,
 * base de datos y distribución de la inversión), haciendo lo que harían la app y Wompi:
 * <ol>
 *   <li>El socio pide el enlace de pago (con su sesión y la firma HMAC de la app).</li>
 *   <li>Wompi solo acepta el checkout si la firma de integridad corresponde a la referencia y el monto.</li>
 *   <li>Wompi avisa el resultado con {@code transaction.updated}, firmado con el secreto de eventos.</li>
 * </ol>
 * Las firmas se calculan aquí por separado (SHA-256 directo) y no con el código de la app, para que
 * un error en ese código no se confirme a sí mismo.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@AutoConfigureMockMvc
class WompiSandboxDepositFlowIntegrationTest {

    private static final long AMOUNT_IN_CENTS = 5_000_000L; // $50.000 COP

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepositoryPort userRepository;
    @Autowired
    private CoreAccountRepositoryPort accountRepository;
    @Autowired
    private CoreTransactionRepositoryPort transactionRepository;
    @Autowired
    private TokenPort tokenPort;
    @Autowired
    private MetSecurityProperties securityProperties;
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${met.wompi.public-key}")
    private String publicKey;
    @Value("${met.wompi.integrity-secret}")
    private String integritySecret;
    @Value("${met.wompi.webhook-secret}")
    private String eventsSecret;

    private record Member(UUID userId, UUID accountId, String accessToken) {
    }

    @Test
    void unPagoAprobadoEnSandboxSeAcreditaUnaSolaVez() throws Exception {
        Member member = givenMember(KycStatus.APPROVED);

        MultiValueMap<String, String> checkout = query(requestDepositLink(member, 50000));
        String reference = checkout.getFirst("reference");
        assertEquals(publicKey, checkout.getFirst("public-key"));
        assertEquals("COP", checkout.getFirst("currency"));
        assertEquals(String.valueOf(AMOUNT_IN_CENTS), checkout.getFirst("amount-in-cents"));
        assertTrue(reference.startsWith("MET-" + member.userId() + "-"), reference);
        assertEquals(sha256(reference + AMOUNT_IN_CENTS + "COP" + integritySecret), checkout.getFirst("signature:integrity"),
                "Wompi rechaza el checkout si la firma de integridad no corresponde");

        String event = wompiEvent("15113-1730000000-11111", "APPROVED", reference, AMOUNT_IN_CENTS);
        sendWebhook(event, checksumOf(event)).andExpect(status().isOk());

        assertEquals(0, new BigDecimal("50000").compareTo(principalOf(member)));
        List<CoreTransaction> deposits = deposits(member);
        assertEquals(1, deposits.size());
        assertEquals("Recarga vía WOMPI_CARD", deposits.get(0).getConcept());

        // Wompi reintenta si no recibe un 200 a tiempo: el mismo evento no puede acreditar dos veces.
        sendWebhook(event, checksumOf(event)).andExpect(status().isOk());
        assertEquals(0, new BigDecimal("50000").compareTo(principalOf(member)));
        assertEquals(1, deposits(member).size());
    }

    @Test
    void rechazaUnEventoConElMontoAlterado() throws Exception {
        Member member = givenMember(KycStatus.APPROVED);
        String reference = query(requestDepositLink(member, 50000)).getFirst("reference");
        String genuine = wompiEvent("15113-1730000000-22222", "APPROVED", reference, AMOUNT_IN_CENTS);
        String tampered = wompiEvent("15113-1730000000-22222", "APPROVED", reference, 500_000_000L);

        sendWebhook(tampered, checksumOf(genuine)).andExpect(status().isUnauthorized());

        assertEquals(0, BigDecimal.ZERO.compareTo(principalOf(member)));
    }

    @Test
    void confirmaUnPagoRechazadoSinAcreditar() throws Exception {
        Member member = givenMember(KycStatus.APPROVED);
        String reference = query(requestDepositLink(member, 50000)).getFirst("reference");
        String event = wompiEvent("15113-1730000000-33333", "DECLINED", reference, AMOUNT_IN_CENTS);

        sendWebhook(event, checksumOf(event)).andExpect(status().isOk());

        assertEquals(0, BigDecimal.ZERO.compareTo(principalOf(member)));
        assertTrue(deposits(member).isEmpty());
    }

    @Test
    void unSocioSinIdentidadAprobadaNoPuedeAbrirElPago() throws Exception {
        // Si pudiera pagar, el webhook después no podría acreditarle (el depósito exige KYC aprobado):
        // Wompi reintentaría tres veces y el dinero quedaría cobrado sin llegar a su cuenta.
        Member member = givenMember(KycStatus.IN_REVIEW);

        String body = "{\"amount\":50000,\"returnUrl\":\"metapp://deposit/success\"}";
        MvcResult result = signedPost("/v1/accounts/deposit-pse", body, member.accessToken())
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertEquals("KYC_REQUIRED", objectMapper.readTree(result.getResponse().getContentAsString()).path("code").asText());
    }

    @Test
    void laPaginaDeRetornoAbreSinSesion() throws Exception {
        mockMvc.perform(get("/v1/payments/wompi/return").servletPath("/v1/payments/wompi/return"))
                .andExpect(status().isOk());
    }

    // ── Pasos del flujo ───────────────────────────────────────────────────────────

    private String requestDepositLink(Member member, int amount) throws Exception {
        String body = "{\"amount\":" + amount + ",\"returnUrl\":\"metapp://deposit/success\"}";
        MvcResult result = signedPost("/v1/accounts/deposit-pse", body, member.accessToken())
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("paymentUrl").asText();
    }

    /** POST como lo envía la app: con la sesión del socio y la firma HMAC de las escrituras. */
    private org.springframework.test.web.servlet.ResultActions signedPost(String path, String body, String accessToken)
            throws Exception {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        return mockMvc.perform(post(path)
                .servletPath(path)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Timestamp", timestamp)
                .header("X-Signature", HmacSigner.sign("POST", path, timestamp, body, hmacSecret()))
                .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions sendWebhook(String event, String checksum) throws Exception {
        return mockMvc.perform(post("/v1/webhooks/payment")
                .servletPath("/v1/webhooks/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Event-Checksum", checksum)
                .content(event.replace("PLACEHOLDER", checksum)));
    }

    /** Evento con la forma que documenta Wompi para {@code transaction.updated}. */
    private static String wompiEvent(String transactionId, String status, String reference, long amountInCents) {
        return """
                {
                  "event": "transaction.updated",
                  "data": {
                    "transaction": {
                      "id": "%s",
                      "amount_in_cents": %d,
                      "reference": "%s",
                      "customer_email": "socio@example.com",
                      "currency": "COP",
                      "payment_method_type": "CARD",
                      "redirect_url": "https://met.example/api/v1/payments/wompi/return",
                      "status": "%s",
                      "shipping_address": null,
                      "payment_link_id": null,
                      "payment_source_id": null
                    }
                  },
                  "environment": "test",
                  "signature": {
                    "properties": ["transaction.id", "transaction.status", "transaction.amount_in_cents"],
                    "checksum": "PLACEHOLDER"
                  },
                  "timestamp": 1730000000,
                  "sent_at": "2026-10-29T15:00:00.000Z"
                }
                """.formatted(transactionId, amountInCents, reference, status);
    }

    /** Checksum de Wompi: valores de signature.properties + timestamp + secreto de eventos, en SHA-256. */
    private String checksumOf(String event) throws Exception {
        JsonNode tx = objectMapper.readTree(event).path("data").path("transaction");
        return sha256(tx.path("id").asText() + tx.path("status").asText() + tx.path("amount_in_cents").asText()
                + "1730000000" + eventsSecret).toUpperCase();
    }

    // ── Datos ─────────────────────────────────────────────────────────────────────

    private Member givenMember(KycStatus kycStatus) {
        Instant now = Instant.now();
        String document = String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L));
        User user = userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .documentType(DocumentType.CC)
                .documentNumber(document)
                .firstName("Socia")
                .lastName("Sandbox")
                .email(document + "@example.com")
                .status(UserStatus.ACTIVE)
                .kycStatus(kycStatus)
                .termsAccepted(true)
                .termsAcceptedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
        CoreAccount account = accountRepository.save(CoreAccount.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .accountNumber("45" + document.substring(0, 8))
                .principalBalance(BigDecimal.ZERO)
                .interestBalance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return new Member(user.getId(), account.getId(), tokenPort.generateAccessToken(user.getId(), user.getEmail()));
    }

    private BigDecimal principalOf(Member member) {
        return accountRepository.findById(member.accountId()).orElseThrow().getPrincipalBalance();
    }

    private List<CoreTransaction> deposits(Member member) {
        return transactionRepository.findByAccountId(member.accountId()).stream()
                .filter(tx -> tx.getType() == TransactionType.DEPOSIT)
                .toList();
    }

    private static MultiValueMap<String, String> query(String url) {
        return UriComponentsBuilder.fromUriString(url).build().getQueryParams();
    }

    private String hmacSecret() {
        String secret = securityProperties.getEncryption().getHmacSecret();
        return secret == null || secret.isBlank() ? securityProperties.getEncryption().getAesKey() : secret;
    }

    private static String sha256(String data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8)));
    }
}
