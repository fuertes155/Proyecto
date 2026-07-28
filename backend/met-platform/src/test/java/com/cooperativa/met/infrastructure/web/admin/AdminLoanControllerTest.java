package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.lending.usecase.AdminLoanUseCase;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminLoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminLoanUseCase adminLoanUseCase;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldGetAllLoans() throws Exception {
        PersonalLoanApplication loan = PersonalLoanApplication.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("1000000"))
                .termMonths(12)
                .annualInterestRate(new BigDecimal("0.18"))
                .monthlyPayment(new BigDecimal("100000"))
                .totalInterest(new BigDecimal("200000"))
                .totalPayment(new BigDecimal("1200000"))
                .purpose("Personal")
                .status(LoanApplicationStatus.SUBMITTED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Mockito.when(adminLoanUseCase.getAllLoans()).thenReturn(List.of(loan));

        mockMvc.perform(get("/v1/admin/loans")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldUpdateLoanStatus() throws Exception {
        UUID loanId = UUID.randomUUID();
        PersonalLoanApplication loan = PersonalLoanApplication.builder()
                .id(loanId)
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("1000000"))
                .termMonths(12)
                .annualInterestRate(new BigDecimal("0.18"))
                .monthlyPayment(new BigDecimal("100000"))
                .totalInterest(new BigDecimal("200000"))
                .totalPayment(new BigDecimal("1200000"))
                .purpose("Personal")
                .status(LoanApplicationStatus.APPROVED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Mockito.when(adminLoanUseCase.updateLoanStatus(eq(loanId), eq(LoanApplicationStatus.APPROVED))).thenReturn(loan);

        mockMvc.perform(put("/v1/admin/loans/" + loanId + "/status?status=APPROVED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
