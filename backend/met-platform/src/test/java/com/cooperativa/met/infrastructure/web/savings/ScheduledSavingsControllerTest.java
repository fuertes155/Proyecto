package com.cooperativa.met.infrastructure.web.savings;

import com.cooperativa.met.application.savings.dto.ContributionResponse;
import com.cooperativa.met.application.savings.dto.CreateScheduledSavingsRequest;
import com.cooperativa.met.application.savings.dto.ScheduledSavingsResponse;
import com.cooperativa.met.application.savings.usecase.CreateScheduledSavingsUseCase;
import com.cooperativa.met.application.savings.usecase.GetContributionHistoryUseCase;
import com.cooperativa.met.application.savings.usecase.GetScheduledSavingsUseCase;
import com.cooperativa.met.application.savings.usecase.ListScheduledSavingsUseCase;
import com.cooperativa.met.application.savings.usecase.UpdateScheduledSavingsUseCase;
import com.cooperativa.met.application.savings.usecase.WithdrawSavingsUseCase;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class ScheduledSavingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateScheduledSavingsUseCase createUseCase;
    @MockBean
    private ListScheduledSavingsUseCase listUseCase;
    @MockBean
    private GetScheduledSavingsUseCase getUseCase;
    @MockBean
    private UpdateScheduledSavingsUseCase updateUseCase;
    @MockBean
    private GetContributionHistoryUseCase historyUseCase;
    @MockBean
    private WithdrawSavingsUseCase withdrawUseCase;

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldCreateScheduledSavings() throws Exception {
        ScheduledSavingsResponse response = new ScheduledSavingsResponse(
                UUID.randomUUID(),
                "Vacaciones",
                "ACTIVE",
                "MONTHLY",
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                LocalDateTime.now().plusMonths(1),
                0.0
        );

        Mockito.when(createUseCase.execute(any(UUID.class), any(CreateScheduledSavingsRequest.class))).thenReturn(response);

        String json = "{\"name\": \"Vacaciones\", \"frequency\": \"MONTHLY\", \"contributionAmount\": 100000}";

        mockMvc.perform(post("/v1/savings/scheduled")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Vacaciones"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldListScheduledSavings() throws Exception {
        ScheduledSavingsResponse response = new ScheduledSavingsResponse(
                UUID.randomUUID(),
                "Vacaciones",
                "ACTIVE",
                "MONTHLY",
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                LocalDateTime.now().plusMonths(1),
                0.0
        );

        Mockito.when(listUseCase.execute(any(UUID.class))).thenReturn(List.of(response));

        mockMvc.perform(get("/v1/savings/scheduled")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Vacaciones"));
    }
}
