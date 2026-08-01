package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.EmergencyLockRequest;
import com.cooperativa.met.application.admin.usecase.EmergencyLockUseCase;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class EmergencyLockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmergencyLockUseCase emergencyLockUseCase;

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = {"ADMIN"})
    void shouldExecuteLock() throws Exception {
        Mockito.doNothing().when(emergencyLockUseCase).execute(any(UUID.class), any(EmergencyLockRequest.class), anyString());

        String json = "{\"scope\": \"USER\", \"targetId\": \"12345678-1234-1234-1234-123456789012\", \"reason\": \"Suspicious activity\"}";

        mockMvc.perform(post("/v1/admin/emergency-lock").with(csrf())
                .servletPath("/v1/admin/emergency-lock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
