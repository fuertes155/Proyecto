package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.FeeScheduleRequest;
import com.cooperativa.met.application.admin.usecase.ManageFeesUseCase;
import com.cooperativa.met.domain.admin.model.FeeSchedule;
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
        FeeSchedule fee = FeeSchedule.builder()
                .id(UUID.randomUUID())
                .tipoTarifa("WITHDRAWAL")
                .descripcion("Tarifa de retiro")
                .valor(new BigDecimal("5000"))
                .esPorcentaje(false)
                .vigentDesde(Instant.now())
                .creadoPor(UUID.randomUUID())
                .createdAt(Instant.now())
                .build();

        Mockito.when(manageFeesUseCase.getAll()).thenReturn(List.of(fee));

        mockMvc.perform(get("/v1/admin/fees")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoTarifa").value("WITHDRAWAL"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = {"ADMIN"})
    void shouldCreateFee() throws Exception {
        FeeSchedule fee = FeeSchedule.builder()
                .id(UUID.randomUUID())
                .tipoTarifa("TRANSFER")
                .descripcion("Tarifa de transferencia")
                .valor(new BigDecimal("1500"))
                .esPorcentaje(false)
                .vigentDesde(Instant.now())
                .creadoPor(UUID.randomUUID())
                .createdAt(Instant.now())
                .build();

        Mockito.when(manageFeesUseCase.create(any(UUID.class), any(FeeScheduleRequest.class), anyString())).thenReturn(fee);

        String json = "{\"tipoTarifa\": \"TRANSFER\", \"valor\": 1500, \"descripcion\": \"Transfer fee\", \"esPorcentaje\": false}";

        mockMvc.perform(post("/v1/admin/fees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoTarifa").value("TRANSFER"));
    }
}
