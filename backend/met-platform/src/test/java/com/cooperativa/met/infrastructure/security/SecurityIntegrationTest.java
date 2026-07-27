package com.cooperativa.met.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.cooperativa.met.TestRedisConfig.class)
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnUnauthorizedWhenAccessingProtectedRouteWithoutToken() throws Exception {
        // Escenario 1: Acceder a una ruta privada sin token debe devolver 401
        mockMvc.perform(get("/v1/accounts/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessingProtectedRouteWithInvalidToken() throws Exception {
        // Escenario 2: Acceder a una ruta privada con un token falso debe devolver 401
        mockMvc.perform(get("/v1/accounts/me")
                .header("Authorization", "Bearer fake-invalid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAccessToPublicRoutesWithoutToken() throws Exception {
        // Escenario 3: Acceder a una ruta pública como el login no debe devolver 401
        // Devolverá 400 Bad Request porque no estamos enviando el body correcto, pero NO 401.
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnForbiddenWhenUserAccessesAdminRoute() throws Exception {
        // Escenario 4: Un usuario normal (rol USER) intentando acceder a una ruta ADMIN debe recibir 403
        mockMvc.perform(get("/v1/admin/loans")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
