package com.cooperativa.met.infrastructure.web.identity;

import com.cooperativa.met.application.identity.dto.LoginRequest;
import com.cooperativa.met.application.identity.dto.AuthResponse;
import com.cooperativa.met.application.identity.usecase.LoginUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoginUseCase loginUseCase;

    @Test
    void shouldReturnTokenOnSuccessfulLogin() throws Exception {
        // Arrange
        AuthResponse mockResponse = AuthResponse.of(UUID.randomUUID(), "mocked-jwt-token", "refresh-token", 3600000L);
        
        Mockito.when(loginUseCase.execute(Mockito.any(LoginRequest.class), Mockito.anyString())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documentType\": \"CC\", \"documentNumber\": \"123456789\", \"pin\": \"1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mocked-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void shouldReturnBadRequestWhenLoginRequestIsInvalid() throws Exception {
        // Arrange: Email is missing in the JSON
        String invalidJson = "{\"password\": \"password123\"}";

        // Act & Assert
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
