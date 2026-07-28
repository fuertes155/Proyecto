package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.identity.usecase.AdminUserUseCase;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserUseCase adminUserUseCase;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldGetAllUsers() throws Exception {
        User user = User.builder()
                .id(UUID.randomUUID())
                .documentType(DocumentType.CC)
                .documentNumber("123456")
                .email("admin@test.com")
                .phone("3001234567")
                .firstName("AdminUser")
                .lastName("Test")
                .pinHash("hash")
                .biometricHash("")
                .failedLoginAttempts(0)
                .status(UserStatus.ACTIVE)
                .kycStatus(KycStatus.APPROVED)
                .termsAccepted(true)
                .termsAcceptedAt(Instant.now())
                .emailNotificationsEnabled(true)
                .pushNotificationsEnabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .lastKnownIp("")
                .paymentCardToken("")
                .lastKnownDeviceId("")
                .build();

        Mockito.when(adminUserUseCase.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("AdminUser"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldUpdateKycStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .documentType(DocumentType.CC)
                .documentNumber("654321")
                .email("user@test.com")
                .phone("3009876543")
                .firstName("Test")
                .lastName("User")
                .pinHash("hash")
                .biometricHash("")
                .failedLoginAttempts(0)
                .status(UserStatus.ACTIVE)
                .kycStatus(KycStatus.APPROVED)
                .termsAccepted(true)
                .termsAcceptedAt(Instant.now())
                .emailNotificationsEnabled(true)
                .pushNotificationsEnabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .lastKnownIp("")
                .paymentCardToken("")
                .lastKnownDeviceId("")
                .build();

        Mockito.when(adminUserUseCase.updateKycStatus(eq(userId), eq(KycStatus.APPROVED))).thenReturn(user);

        mockMvc.perform(put("/v1/admin/users/" + userId + "/kyc?status=APPROVED").with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("APPROVED"));
    }
}
