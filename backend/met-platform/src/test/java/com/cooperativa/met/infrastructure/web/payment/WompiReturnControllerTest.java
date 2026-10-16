package com.cooperativa.met.infrastructure.web.payment;

import com.cooperativa.met.domain.bank.port.PayoutGatewayPort;
import com.cooperativa.met.domain.bank.port.PseGatewayPort;
import com.cooperativa.met.infrastructure.adapter.wompi.WompiPayoutAdapter;
import com.cooperativa.met.infrastructure.adapter.wompi.WompiPseAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.cooperativa.met.TestRedisConfig.class)
@AutoConfigureMockMvc
class WompiReturnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Test
    void returnPageIsPublicStaticAndHasItsOwnCsp() throws Exception {
        mockMvc.perform(get("/v1/payments/wompi/return").servletPath("/v1/payments/wompi/return")
                        .param("id", "<script>alert(1)</script>"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("Estamos confirmando tu pago")))
                .andExpect(content().string(not(containsString("<script>"))))
                .andExpect(header().string("Content-Security-Policy", WompiReturnController.CONTENT_SECURITY_POLICY));
    }

    @Test
    void signedWompiEventPassesTheSecurityFiltersWithoutJwt() throws Exception {
        // Firmado con met.wompi.webhook-secret de application-test.yml:
        // printf '%s' "tx-42DECLINED50000001735000000test-webhook-secret" | sha256sum
        String declinedEvent = """
                { "event": "transaction.updated",
                  "data": { "transaction": { "id": "tx-42", "status": "DECLINED", "amount_in_cents": 5000000,
                            "reference": "MET-550e8400-e29b-41d4-a716-446655440000-1", "currency": "COP" } },
                  "environment": "test",
                  "signature": { "properties": ["transaction.id", "transaction.status", "transaction.amount_in_cents"],
                                 "checksum": "5AAC26CB7E808F95F086116141CEC652A325CB11B558B9E3FAC9FE6233815627" },
                  "timestamp": 1735000000 }
                """;

        // servletPath como en Tomcat (DispatcherServlet en "/"), igual que los demás tests de seguridad.
        mockMvc.perform(post("/v1/webhooks/payment").servletPath("/v1/webhooks/payment")
                        .contentType("application/json").content(declinedEvent))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/webhooks/payment").servletPath("/v1/webhooks/payment").contentType("application/json")
                        .content(declinedEvent.replace("5AAC26CB", "00000000")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void realWompiAdaptersAreWiredByDefault() {
        assertInstanceOf(WompiPseAdapter.class, context.getBean(PseGatewayPort.class));
        assertInstanceOf(WompiPayoutAdapter.class, context.getBean(PayoutGatewayPort.class));
    }
}
