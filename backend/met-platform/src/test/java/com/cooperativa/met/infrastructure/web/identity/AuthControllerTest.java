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
@org.springframework.test.context.ActiveProfiles("test")
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
                .content("{\"documentType\": \"CC\", \"documentNumber\": \"123456789\", \"pin\": \"1234\", \"deviceId\": \"test-device-id\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mocked-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @MockBean
    private com.cooperativa.met.application.identity.usecase.RegisterUserUseCase registerUserUseCase;

    @MockBean
    private com.cooperativa.met.application.identity.usecase.LogoutUseCase logoutUseCase;

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

    @Test
    void shouldRegisterUser() throws Exception {
        com.cooperativa.met.application.identity.dto.UserResponse mockResponse = new com.cooperativa.met.application.identity.dto.UserResponse(
            UUID.randomUUID(),
            "John",
            "Doe",
            "john@example.com",
            "+1234567890",
            "CC",
            "1234567890",
            "VERIFIED",
            java.time.LocalDateTime.now()
        );

        Mockito.when(registerUserUseCase.execute(Mockito.any())).thenReturn(mockResponse);

        String json = "{\"firstName\": \"John\", \"lastName\": \"Doe\", \"email\": \"john@example.com\", \"phone\": \"+1234567890\", \"documentType\": \"CC\", \"documentNumber\": \"1234567890\", \"pin\": \"1234\", \"deviceId\": \"test\"}";

        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldLogoutUser() throws Exception {
        Mockito.doNothing().when(logoutUseCase).execute(Mockito.any(), Mockito.anyString());

        mockMvc.perform(post("/v1/auth/logout")
                .header("Authorization", "Bearer mock-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
