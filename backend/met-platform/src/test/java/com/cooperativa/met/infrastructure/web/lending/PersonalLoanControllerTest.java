package com.cooperativa.met.infrastructure.web.lending;

import com.cooperativa.met.application.lending.dto.LoanApplicationResponse;
import com.cooperativa.met.application.lending.dto.LoanSimulationResponse;
import com.cooperativa.met.application.lending.dto.SimulateLoanRequest;
import com.cooperativa.met.application.lending.dto.SubmitLoanApplicationRequest;
import com.cooperativa.met.application.lending.usecase.GetPersonalLoanApplicationUseCase;
import com.cooperativa.met.application.lending.usecase.ListPersonalLoanApplicationsUseCase;
import com.cooperativa.met.application.lending.usecase.SimulatePersonalLoanUseCase;
import com.cooperativa.met.application.lending.usecase.SubmitPersonalLoanApplicationUseCase;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
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

    @MockBean
    private SimulatePersonalLoanUseCase simulateUseCase;

    @MockBean
    private SubmitPersonalLoanApplicationUseCase submitUseCase;

    @MockBean
    private ListPersonalLoanApplicationsUseCase listUseCase;

    @MockBean
    private GetPersonalLoanApplicationUseCase getUseCase;

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

        mockMvc.perform(post("/v1/loans/simulate").with(csrf())
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

        String json = "{\"amount\": 1000000, \"termMonths\": 12, \"purpose\": \"EDUCATION\"}";

        mockMvc.perform(post("/v1/loans/applications").with(csrf())
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
}
