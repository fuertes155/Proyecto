package com.cooperativa.met.infrastructure.adapter.wompi;

import com.cooperativa.met.domain.bank.port.PseFinancialInstitution;
import com.cooperativa.met.domain.bank.port.PseTransactionRequest;
import com.cooperativa.met.domain.bank.port.PseTransactionResult;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WompiPseAdapterTest {

    private static final String BASE = "https://sandbox.wompi.co/v1";
    private static final String PUBLIC_KEY = "pub_test_abc";
    private static final String PRIVATE_KEY = "prv_test_xyz";
    private static final String INTEGRITY = "test_integrity_abc123";
    private static final String REFERENCE = "MET-ref-1735000000000";

    private static final String MERCHANT = """
            { "data": {
                "presigned_acceptance": { "acceptance_token": "acc-token", "permalink": "https://x/terms.pdf" },
                "presigned_personal_data_auth": { "acceptance_token": "pda-token", "permalink": "https://x/data.pdf" }
            } }
            """;

    private MockRestServiceServer server;
    private WompiPseAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new WompiPseAdapter(builder.build());
        ReflectionTestUtils.setField(adapter, "publicKey", PUBLIC_KEY);
        ReflectionTestUtils.setField(adapter, "privateKey", PRIVATE_KEY);
        ReflectionTestUtils.setField(adapter, "integritySecret", INTEGRITY);
        ReflectionTestUtils.setField(adapter, "pollAttempts", 3);
        ReflectionTestUtils.setField(adapter, "pollIntervalMs", 0L);
    }

    @Test
    void createsSignedPseTransactionAndWaitsForTheBankUrl() {
        server.expect(requestTo(BASE + "/merchants/info"))
                .andExpect(header("x-merchant-public-key", PUBLIC_KEY))
                .andRespond(withSuccess(MERCHANT, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/transactions"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + PRIVATE_KEY))
                .andExpect(jsonPath("$.acceptance_token").value("acc-token"))
                .andExpect(jsonPath("$.accept_personal_auth").value("pda-token"))
                .andExpect(jsonPath("$.amount_in_cents").value(5_000_000))
                .andExpect(jsonPath("$.currency").value("COP"))
                // printf '%s' "MET-ref-1735000000000""5000000""COP""test_integrity_abc123" | sha256sum
                .andExpect(jsonPath("$.signature").value("dc63dc1d2a8553642cdca5fbf663d36abfe42258d52ec534366201a7e6703e17"))
                .andExpect(jsonPath("$.reference").value(REFERENCE))
                .andExpect(jsonPath("$.payment_method.type").value("PSE"))
                .andExpect(jsonPath("$.payment_method.user_type").value(0))
                .andExpect(jsonPath("$.payment_method.user_legal_id_type").value("PP"))
                .andExpect(jsonPath("$.payment_method.financial_institution_code").value("1"))
                .andExpect(jsonPath("$.customer_data.full_name").value("Ana Pérez"))
                .andExpect(jsonPath("$.customer_data.phone_number").value("573001234567"))
                .andRespond(withSuccess("""
                        { "data": { "id": "tx-1", "status": "PENDING", "payment_method": { "type": "PSE", "extra": {} } } }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/transactions/tx-1"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + PRIVATE_KEY))
                .andRespond(withSuccess("""
                        { "data": { "id": "tx-1", "status": "PENDING",
                          "payment_method": { "type": "PSE", "extra": { "async_payment_url": "https://bank.test/pse/tx-1" } } } }
                        """, MediaType.APPLICATION_JSON));

        PseTransactionResult result = adapter.createPseTransaction(request("PASAPORTE", "300 123 4567"));

        assertEquals("tx-1", result.transactionId());
        assertEquals("https://bank.test/pse/tx-1", result.asyncPaymentUrl());
        assertEquals("PENDING", result.status());
        server.verify();
    }

    @Test
    void fallsBackToLegacyMerchantEndpointWhenInfoIsNotAvailable() {
        server.expect(requestTo(BASE + "/merchants/info")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(BASE + "/merchants/" + PUBLIC_KEY))
                .andRespond(withSuccess(MERCHANT, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/transactions"))
                .andExpect(jsonPath("$.acceptance_token").value("acc-token"))
                .andRespond(withSuccess("""
                        { "data": { "id": "tx-2", "status": "PENDING",
                          "payment_method": { "extra": { "async_payment_url": "https://bank.test/pse/tx-2" } } } }
                        """, MediaType.APPLICATION_JSON));

        PseTransactionResult result = adapter.createPseTransaction(request("CC", null));

        assertEquals("https://bank.test/pse/tx-2", result.asyncPaymentUrl());
        server.verify();
    }

    @Test
    void declinedTransactionFailsWithWompiReason() {
        server.expect(requestTo(BASE + "/merchants/info")).andRespond(withSuccess(MERCHANT, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/transactions"))
                .andRespond(withSuccess("""
                        { "data": { "id": "tx-3", "status": "DECLINED", "status_message": "Banco que rechaza" } }
                        """, MediaType.APPLICATION_JSON));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> adapter.createPseTransaction(request("CC", null)));

        assertTrue(ex.getMessage().contains("Banco que rechaza"), ex.getMessage());
    }

    @Test
    void validationErrorFromWompiBecomesBusinessError() {
        server.expect(requestTo(BASE + "/merchants/info")).andRespond(withSuccess(MERCHANT, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/transactions"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"type\":\"INPUT_VALIDATION_ERROR\",\"messages\":{\"signature\":[\"inválida\"]}}}"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> adapter.createPseTransaction(request("CC", null)));

        assertTrue(ex.getMessage().contains("422"), ex.getMessage());
    }

    @Test
    void givesUpWhenTheBankUrlNeverArrives() {
        server.expect(requestTo(BASE + "/merchants/info")).andRespond(withSuccess(MERCHANT, MediaType.APPLICATION_JSON));
        String pending = "{ \"data\": { \"id\": \"tx-4\", \"status\": \"PENDING\", \"payment_method\": { \"extra\": {} } } }";
        server.expect(requestTo(BASE + "/transactions")).andRespond(withSuccess(pending, MediaType.APPLICATION_JSON));
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo(BASE + "/transactions/tx-4")).andRespond(withSuccess(pending, MediaType.APPLICATION_JSON));
        }

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> adapter.createPseTransaction(request("CC", null)));

        assertEquals("WOMPI_PSE_TIMEOUT", ex.getCode());
        server.verify();
    }

    @Test
    void listsPseBanksWithThePublicKey() {
        server.expect(requestTo(BASE + "/pse/financial_institutions"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + PUBLIC_KEY))
                .andRespond(withSuccess("""
                        { "data": [
                          { "financial_institution_code": "1", "financial_institution_name": "Banco que aprueba" },
                          { "financial_institution_code": "2", "financial_institution_name": "Banco que rechaza" }
                        ] }
                        """, MediaType.APPLICATION_JSON));

        List<PseFinancialInstitution> banks = adapter.fetchFinancialInstitutions();

        assertEquals(List.of(new PseFinancialInstitution("1", "Banco que aprueba"),
                new PseFinancialInstitution("2", "Banco que rechaza")), banks);
    }

    private static PseTransactionRequest request(String documentType, String phone) {
        return new PseTransactionRequest(REFERENCE, new BigDecimal("50000"), "ana@example.com", "Ana Pérez",
                phone, documentType, "123456789", "1", "https://met.test/api/v1/payments/wompi/return", "Recarga MET");
    }
}
