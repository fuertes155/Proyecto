package com.cooperativa.met.infrastructure.web.investment;

import com.cooperativa.met.application.investment.dto.CreatePortfolioRequest;
import com.cooperativa.met.application.investment.dto.PortfolioResponse;
import com.cooperativa.met.application.investment.usecase.CancelMicroInvestmentUseCase;
import com.cooperativa.met.application.investment.usecase.CreateMicroInvestmentPortfolioUseCase;
import com.cooperativa.met.application.investment.usecase.GetInvestmentPortfolioUseCase;
import com.cooperativa.met.application.investment.usecase.GetInvestmentReturnsUseCase;
import com.cooperativa.met.application.investment.usecase.ListInvestmentInstrumentsUseCase;
import com.cooperativa.met.domain.investment.model.InvestmentInstrument;
import com.cooperativa.met.domain.investment.model.InvestmentReturn;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class MicroInvestmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListInvestmentInstrumentsUseCase listInstrumentsUseCase;

    @MockBean
    private CreateMicroInvestmentPortfolioUseCase createPortfolioUseCase;

    @MockBean
    private GetInvestmentPortfolioUseCase getPortfolioUseCase;

    @MockBean
    private CancelMicroInvestmentUseCase cancelUseCase;

    @MockBean
    private GetInvestmentReturnsUseCase getReturnsUseCase;

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldListInstruments() throws Exception {
        InvestmentInstrument instrument = new InvestmentInstrument();
        instrument.setId(UUID.randomUUID());
        instrument.setName("Agro-Inversión");
        instrument.setRiskLevel("LOW");
        instrument.setStatus("ACTIVE");

        Mockito.when(listInstrumentsUseCase.listActivos()).thenReturn(List.of(instrument));

        mockMvc.perform(get("/v1/investments/instruments")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Agro-Inversión"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldCreatePortfolio() throws Exception {
        PortfolioResponse response = new PortfolioResponse(
                UUID.randomUUID(),
                "ACTIVE",
                new BigDecimal("500000"),
                LocalDateTime.now(),
                null
        );

        Mockito.when(createPortfolioUseCase.execute(any(UUID.class), any(CreatePortfolioRequest.class)))
                .thenReturn(response);

        String json = "{\"totalAmount\": 500000}";

        mockMvc.perform(post("/v1/investments/portfolio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldCancelPortfolio() throws Exception {
        UUID portfolioId = UUID.randomUUID();

        Mockito.doNothing().when(cancelUseCase).execute(any(UUID.class), any(UUID.class));

        mockMvc.perform(delete("/v1/investments/portfolio/" + portfolioId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
