package com.cooperativa.met.infrastructure.web.lending;

import com.cooperativa.met.application.lending.dto.LoanApplicationResponse;
import com.cooperativa.met.application.lending.dto.LoanEligibilityRequest;
import com.cooperativa.met.application.lending.dto.LoanEligibilityResponse;
import com.cooperativa.met.application.lending.dto.LoanSimulationResponse;
import com.cooperativa.met.application.lending.dto.SimulateLoanRequest;
import com.cooperativa.met.application.lending.dto.SubmitLoanApplicationRequest;
import com.cooperativa.met.application.lending.usecase.GetLoanEligibilityUseCase;
import com.cooperativa.met.application.lending.usecase.GetPersonalLoanApplicationUseCase;
import com.cooperativa.met.application.lending.usecase.ListPersonalLoanApplicationsUseCase;
import com.cooperativa.met.application.lending.usecase.SimulatePersonalLoanUseCase;
import com.cooperativa.met.application.lending.usecase.SubmitPersonalLoanApplicationUseCase;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import com.cooperativa.met.infrastructure.security.HmacSigner;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class PersonalLoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MetSecurityProperties securityProperties;

    @MockBean
    private SimulatePersonalLoanUseCase simulateUseCase;

    @MockBean
    private SubmitPersonalLoanApplicationUseCase submitUseCase;

    @MockBean
    private ListPersonalLoanApplicationsUseCase listUseCase;

    @MockBean
    private GetPersonalLoanApplicationUseCase getUseCase;

    @MockBean
    private GetLoanEligibilityUseCase eligibilityUseCase;

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldReturnEligibility() throws Exception {
        LoanEligibilityResponse response = new LoanEligibilityResponse(
                true, "RIESGO_MEDIO", 750, new BigDecimal("10000000"), 24, new BigDecimal("0.22"), null);

        Mockito.when(eligibilityUseCase.execute(any(UUID.class), any(LoanEligibilityRequest.class)))
                .thenReturn(response);

        String body = "{\"acceptedHabeasData\": true}";
        String timestamp = String.valueOf(System.currentTimeMillis());
        mockMvc.perform(post("/v1/loans/eligibility").with(csrf())
                .servletPath("/v1/loans/eligibility")
                .header("X-Signature", sign("POST", "/v1/loans/eligibility", timestamp, body))
                .header("X-Timestamp", timestamp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.tier").value("RIESGO_MEDIO"))
                .andExpect(jsonPath("$.maxAmount").value(10000000));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldSimulateLoan() throws Exception {
        LoanSimulationResponse response = new LoanSimulationResponse(
                new BigDecimal("1000000"),
                12,
                new BigDecimal("0.18"),
                new BigDecimal("0.015"),
                new BigDecimal("95000"),
                new BigDecimal("140000"),
                new BigDecimal("1140000"),
                List.of()
        );

        Mockito.when(simulateUseCase.execute(any(SimulateLoanRequest.class))).thenReturn(response);

        String json = "{\"amount\": 1000000, \"termMonths\": 12, \"purpose\": \"EDUCATION\"}";
        String timestamp = String.valueOf(System.currentTimeMillis());

        mockMvc.perform(post("/v1/loans/simulate").with(csrf())
                .servletPath("/v1/loans/simulate")
                .header("X-Signature", sign("POST", "/v1/loans/simulate", timestamp, json))
                .header("X-Timestamp", timestamp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPayment").value(95000));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldSubmitApplication() throws Exception {
        LoanApplicationResponse response = new LoanApplicationResponse(
                UUID.randomUUID(),
                new BigDecimal("1000000"),
                12,
                new BigDecimal("0.18"),
                new BigDecimal("95000"),
                new BigDecimal("140000"),
                new BigDecimal("1140000"),
                "EDUCATION",
                LoanApplicationStatus.SUBMITTED,
                null,
                null,
                Instant.now(),
                List.of()
        );

        Mockito.when(submitUseCase.execute(any(UUID.class), any(SubmitLoanApplicationRequest.class))).thenReturn(response);

        String json = "{\"amount\": 1000000, \"termMonths\": 12, \"purpose\": \"EDUCATION\", \"hasAcceptedHabeasData\": true}";
        String timestamp = String.valueOf(System.currentTimeMillis());

        mockMvc.perform(post("/v1/loans/applications").with(csrf())
                .servletPath("/v1/loans/applications")
                .header("X-Signature", sign("POST", "/v1/loans/applications", timestamp, json))
                .header("X-Timestamp", timestamp)
                .header("X-Device-Attestation", "test-attestation-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldListApplications() throws Exception {
        LoanApplicationResponse response = new LoanApplicationResponse(
                UUID.randomUUID(),
                new BigDecimal("1000000"),
                12,
                new BigDecimal("0.18"),
                new BigDecimal("95000"),
                new BigDecimal("140000"),
                new BigDecimal("1140000"),
                "EDUCATION",
                LoanApplicationStatus.APPROVED,
                null,
                null,
                Instant.now(),
                List.of()
        );

        Mockito.when(listUseCase.execute(any(UUID.class))).thenReturn(List.of(response));

        mockMvc.perform(get("/v1/loans/applications")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    private String sign(String method, String path, String timestamp, String body) {
        String secret = securityProperties.getEncryption().getHmacSecret();
        if (secret == null || secret.isBlank()) {
            secret = securityProperties.getEncryption().getAesKey();
        }
        return HmacSigner.sign(method, path, timestamp, body, secret);
    }
}
