package com.cooperativa.met.infrastructure.adapter.wompi;

import com.cooperativa.met.domain.bank.port.PayoutGatewayBank;
import com.cooperativa.met.domain.bank.port.PayoutGatewayRequest;
import com.cooperativa.met.domain.bank.port.PayoutGatewayResult;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Cliente HTTP de Wompi "Pagos a Terceros", contra un servidor simulado. */
class WompiPayoutAdapterTest {

    private static final String BASE = "https://payouts.wompi.test";

    private MockRestServiceServer server;
    private WompiPayoutAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new WompiPayoutAdapter(builder.build());
        ReflectionTestUtils.setField(adapter, "sourceAccountId", "acc-origen-1");
    }

    @Test
    void shouldSendThePayoutInCentsWithTheBeneficiaryKycData() {
        server.expect(requestTo(BASE + "/payouts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.amountInCents").value(15000000))
                .andExpect(jsonPath("$.accountType").value("AHORROS"))
                .andExpect(jsonPath("$.bankId").value("1507"))
                .andExpect(jsonPath("$.identification.number").value("1086578123"))
                .andExpect(jsonPath("$.beneficiaryName").value("Ana Pérez"))
                .andExpect(jsonPath("$.sourceAccountId").value("acc-origen-1"))
                .andRespond(withSuccess("{\"id\":\"wompi-payout-77\",\"status\":\"PENDING\"}", MediaType.APPLICATION_JSON));

        PayoutGatewayResult result = adapter.initiatePayout(request("SAVINGS"));

        assertEquals(PayoutGatewayResult.PayoutGatewayStatus.ACCEPTED, result.status());
        assertEquals("wompi-payout-77", result.railReference());
        server.verify();
    }

    @Test
    void shouldUseOurReferenceWhenTheProviderDoesNotReturnAnId() {
        server.expect(requestTo(BASE + "/payouts"))
                .andExpect(jsonPath("$.accountType").value("CORRIENTE"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        PayoutGatewayResult result = adapter.initiatePayout(request("CHECKING"));

        assertEquals("PAYOUT-abc", result.railReference());
    }

    @Test
    void shouldReportSynchronousRejectionWithTheProviderStatus() {
        server.expect(requestTo(BASE + "/payouts"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body("{\"error\":\"INVALID_ACCOUNT\"}").contentType(MediaType.APPLICATION_JSON));

        PayoutGatewayResult result = adapter.initiatePayout(request("SAVINGS"));

        assertEquals(PayoutGatewayResult.PayoutGatewayStatus.REJECTED, result.status());
        assertEquals("PROVIDER_422", result.failureCode());
        assertNull(result.railReference());
    }

    @Test
    void shouldAlsoReportServerErrorsAsRejected() {
        // Comportamiento actual: un 5xx se trata como rechazo y el caso de uso devuelve el dinero,
        // aunque el proveedor pudo haber procesado el pago (resultado incierto).
        server.expect(requestTo(BASE + "/payouts")).andRespond(withServerError());

        assertEquals("PROVIDER_500", adapter.initiatePayout(request("SAVINGS")).failureCode());
    }

    @Test
    void shouldReadTheBankCatalogSkippingIncompleteEntries() {
        server.expect(requestTo(BASE + "/banks")).andRespond(withSuccess("""
                [{"id":"1507","name":"Bancolombia"},{"bankId":"1051","name":"Davivienda"},{"name":"Sin id"}]
                """, MediaType.APPLICATION_JSON));

        List<PayoutGatewayBank> banks = adapter.fetchSupportedBanks();

        assertEquals(2, banks.size());
        assertEquals("1051", banks.get(1).providerBankId());
    }

    @Test
    void shouldExplainWhenTheCatalogIsUnavailable() {
        server.expect(requestTo(BASE + "/banks")).andRespond(withServerError());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> adapter.fetchSupportedBanks());

        assertEquals("WOMPI_BANKS_UNAVAILABLE", ex.getCode());
    }

    private PayoutGatewayRequest request(String accountType) {
        return new PayoutGatewayRequest("PAYOUT-abc", new BigDecimal("150000"), "1507", accountType, "00123456789",
                "CC", "1086578123", "Ana Pérez", "socio@example.com", UUID.randomUUID());
    }
}
