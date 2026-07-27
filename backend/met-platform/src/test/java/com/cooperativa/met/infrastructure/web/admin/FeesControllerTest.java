package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.FeeScheduleRequest;
import com.cooperativa.met.application.admin.usecase.ManageFeesUseCase;
import com.cooperativa.met.domain.admin.model.FeeSchedule;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class FeesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManageFeesUseCase manageFeesUseCase;

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = {"ADMIN"})
    void shouldGetAllFees() throws Exception {
        FeeSchedule fee = new FeeSchedule();
        fee.setId(UUID.randomUUID());
        fee.setFeeType("WITHDRAWAL");
        fee.setAmount(new BigDecimal("5000"));

        Mockito.when(manageFeesUseCase.getAll()).thenReturn(List.of(fee));

        mockMvc.perform(get("/v1/admin/fees")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].feeType").value("WITHDRAWAL"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = {"ADMIN"})
    void shouldCreateFee() throws Exception {
        FeeSchedule fee = new FeeSchedule();
        fee.setId(UUID.randomUUID());
        fee.setFeeType("TRANSFER");
        fee.setAmount(new BigDecimal("1500"));

        Mockito.when(manageFeesUseCase.create(any(UUID.class), any(FeeScheduleRequest.class), anyString())).thenReturn(fee);

        String json = "{\"feeType\": \"TRANSFER\", \"amount\": 1500, \"description\": \"Transfer fee\"}";

        mockMvc.perform(post("/v1/admin/fees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feeType").value("TRANSFER"));
    }
}
