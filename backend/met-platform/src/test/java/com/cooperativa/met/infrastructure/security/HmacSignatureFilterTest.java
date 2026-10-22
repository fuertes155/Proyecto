package com.cooperativa.met.infrastructure.security;

import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Firma HMAC de las escrituras de la app móvil: qué exige, qué rechaza y qué rutas quedan fuera.
 */
class HmacSignatureFilterTest {

    private static final String SECRET = "hmac-secret-de-pruebas";
    private static final String BODY = "{\"amount\":150000}";

    private HmacSignatureFilter filter;

    @BeforeEach
    void setUp() {
        MetSecurityProperties properties = new MetSecurityProperties();
        properties.getEncryption().setHmacSecret(SECRET);
        filter = new HmacSignatureFilter(properties);
    }

    private MockHttpServletRequest post(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api" + path);
        request.setContextPath("/api");
        request.setServletPath(path);
        request.setContent(BODY.getBytes(StandardCharsets.UTF_8));
        request.setContentType("application/json");
        return request;
    }

    private void sign(MockHttpServletRequest request, String path, long timestamp, String secret) {
        String ts = String.valueOf(timestamp);
        request.addHeader("X-Timestamp", ts);
        request.addHeader("X-Signature", HmacSigner.sign("POST", path, ts, BODY, secret));
    }

    private record Outcome(int status, boolean passedThrough) {
    }

    private Outcome run(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return new Outcome(response.getStatus(), chain.getRequest() != null);
    }

    @Test
    void acceptsAWriteSignedWithTheSharedSecret() throws Exception {
        MockHttpServletRequest request = post("/v1/accounts/transactions/transfer");
        sign(request, "/v1/accounts/transactions/transfer", Instant.now().toEpochMilli(), SECRET);

        Outcome outcome = run(request);

        assertTrue(outcome.passedThrough());
        assertEquals(200, outcome.status());
    }

    @Test
    void rejectsAnUnsignedWrite() throws Exception {
        Outcome outcome = run(post("/v1/accounts/transactions/transfer"));

        assertFalse(outcome.passedThrough());
        assertEquals(401, outcome.status());
    }

    @Test
    void rejectsASignatureMadeWithAnotherSecret() throws Exception {
        MockHttpServletRequest request = post("/v1/accounts/transactions/transfer");
        sign(request, "/v1/accounts/transactions/transfer", Instant.now().toEpochMilli(), "otro-secreto");

        Outcome outcome = run(request);

        assertFalse(outcome.passedThrough());
        assertEquals(403, outcome.status());
    }

    @Test
    void rejectsAReplayedRequestWithAnOldTimestamp() throws Exception {
        MockHttpServletRequest request = post("/v1/accounts/transactions/transfer");
        sign(request, "/v1/accounts/transactions/transfer", Instant.now().minusSeconds(120).toEpochMilli(), SECRET);

        Outcome outcome = run(request);

        assertFalse(outcome.passedThrough());
        assertEquals(401, outcome.status());
    }

    @Test
    void readsAndWebClientRoutesDoNotNeedASignature() throws Exception {
        MockHttpServletRequest read = new MockHttpServletRequest("GET", "/api/v1/accounts/me");
        read.setServletPath("/v1/accounts/me");
        assertTrue(run(read).passedThrough());

        assertTrue(run(post("/v1/auth/login")).passedThrough());
        assertTrue(run(post("/v1/admin/limits")).passedThrough());
        assertTrue(run(post("/v1/webhooks/wompi")).passedThrough());
    }

    @Test
    void thePublicLoanSimulatorWorksFromTheWebsiteWithoutTheMobileSecret() throws Exception {
        // El simulador del sitio comercial es un cálculo sin efectos y público (permitAll). Un sitio
        // web no puede guardar el secreto de la app: si exigiera firma, el simulador nunca funcionaría.
        Outcome outcome = run(post("/v1/loans/simulate"));

        assertTrue(outcome.passedThrough());
        assertEquals(200, outcome.status());
    }
}
