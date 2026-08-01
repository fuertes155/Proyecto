package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.OperationLimitRequest;
import com.cooperativa.met.application.admin.usecase.ManageOperationLimitsUseCase;
import com.cooperativa.met.domain.admin.model.OperationLimit;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
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
        OperationLimit limit = OperationLimit.builder()
                .id(UUID.randomUUID())
                .tipoOperacion("TRANSFER")
                .montoDiarioMax(1_000_000L)
                .montoPorTransaccionMax(500_000L)
                .activo(true)
                .creadoPor(UUID.randomUUID())
                .updatedAt(Instant.now())
                .build();

        Mockito.when(manageOperationLimitsUseCase.getAll()).thenReturn(List.of(limit));

        mockMvc.perform(get("/v1/admin/limits")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoOperacion").value("TRANSFER"));
    }

    private UsernamePasswordAuthenticationToken jwtAdminAuth() {
        // Replica cómo JwtAuthenticationFilter arma la autenticación en producción: el
        // principal ES el UUID directamente (no un UserDetails), que es lo que exige
        // (UUID) auth.getPrincipal() en el controlador. @WithMockUser no lo replica.
        return new UsernamePasswordAuthenticationToken(
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void shouldUpdateLimit() throws Exception {
        OperationLimit limit = OperationLimit.builder()
                .id(UUID.randomUUID())
                .tipoOperacion("TRANSFER")
                .montoDiarioMax(2_000_000L)
                .montoPorTransaccionMax(1_000_000L)
                .activo(true)
                .creadoPor(UUID.randomUUID())
                .updatedAt(Instant.now())
                .build();

        Mockito.when(manageOperationLimitsUseCase.update(any(UUID.class), any(OperationLimitRequest.class), anyString())).thenReturn(limit);

        String json = "{\"tipoOperacion\": \"TRANSFER\", \"montoDiarioMax\": 2000000, \"montoPorTransaccionMax\": 1000000}";

        mockMvc.perform(put("/v1/admin/limits").with(csrf())
                .with(authentication(jwtAdminAuth()))
                .servletPath("/v1/admin/limits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoOperacion").value("TRANSFER"));
    }
}
