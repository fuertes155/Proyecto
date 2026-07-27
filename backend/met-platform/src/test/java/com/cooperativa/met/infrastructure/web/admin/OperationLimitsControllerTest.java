package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.OperationLimitRequest;
import com.cooperativa.met.application.admin.usecase.ManageOperationLimitsUseCase;
import com.cooperativa.met.domain.admin.model.OperationLimit;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class OperationLimitsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManageOperationLimitsUseCase manageOperationLimitsUseCase;

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = {"ADMIN"})
    void shouldGetAllLimits() throws Exception {
        OperationLimit limit = new OperationLimit();
        limit.setId(UUID.randomUUID());
        limit.setOperationType("TRANSFER");
        limit.setDailyLimit(new BigDecimal("1000000"));

        Mockito.when(manageOperationLimitsUseCase.getAll()).thenReturn(List.of(limit));

        mockMvc.perform(get("/v1/admin/limits")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].operationType").value("TRANSFER"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = {"ADMIN"})
    void shouldUpdateLimit() throws Exception {
        OperationLimit limit = new OperationLimit();
        limit.setId(UUID.randomUUID());
        limit.setOperationType("TRANSFER");

        Mockito.when(manageOperationLimitsUseCase.update(any(UUID.class), any(OperationLimitRequest.class), anyString())).thenReturn(limit);

        String json = "{\"operationType\": \"TRANSFER\", \"dailyLimit\": 2000000, \"monthlyLimit\": 50000000}";

        mockMvc.perform(put("/v1/admin/limits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("TRANSFER"));
    }
}
