package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.usecase.GetAuditLogUseCase;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetAuditLogUseCase getAuditLogUseCase;

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = {"ADMIN"})
    void shouldGetAllAuditLogs() throws Exception {
        AdminAuditEntry entry = AdminAuditEntry.builder()
                .id(UUID.randomUUID())
                .actorAdminId(UUID.randomUUID())
                .accion("CREATE_FEE")
                .entidadAfectada("FeeSchedule")
                .idEntidad(UUID.randomUUID().toString())
                .valoresAnteriores("{}")
                .valoresNuevos("{\"valor\":5000}")
                .motivo("Test")
                .ipOrigen("127.0.0.1")
                .timestamp(Instant.now())
                .build();

        Mockito.when(getAuditLogUseCase.getAll(anyInt(), anyInt())).thenReturn(List.of(entry));
        Mockito.when(getAuditLogUseCase.count()).thenReturn(1L);

        mockMvc.perform(get("/v1/admin/audit-log")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].accion").value("CREATE_FEE"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = {"ADMIN"})
    void shouldGetByAdmin() throws Exception {
        AdminAuditEntry entry = AdminAuditEntry.builder()
                .id(UUID.randomUUID())
                .actorAdminId(UUID.randomUUID())
                .accion("UPDATE_LIMIT")
                .entidadAfectada("OperationLimit")
                .idEntidad(UUID.randomUUID().toString())
                .valoresAnteriores("{}")
                .valoresNuevos("{\"montoDiarioMax\":2000000}")
                .motivo("Test")
                .ipOrigen("127.0.0.1")
                .timestamp(Instant.now())
                .build();

        Mockito.when(getAuditLogUseCase.getByAdmin(anyString(), anyInt(), anyInt())).thenReturn(List.of(entry));

        mockMvc.perform(get("/v1/admin/audit-log/admin/123e4567-e89b-12d3-a456-426614174000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accion").value("UPDATE_LIMIT"));
    }
}
