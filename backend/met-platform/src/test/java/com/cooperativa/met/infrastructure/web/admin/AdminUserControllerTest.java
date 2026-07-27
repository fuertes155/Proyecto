package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.identity.usecase.AdminUserUseCase;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

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
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("AdminUser");

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
        User user = new User();
        user.setId(userId);
        user.setKycStatus(KycStatus.VERIFIED);

        Mockito.when(adminUserUseCase.updateKycStatus(eq(userId), eq(KycStatus.VERIFIED))).thenReturn(user);

        mockMvc.perform(put("/v1/admin/users/" + userId + "/kyc?status=VERIFIED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("VERIFIED"));
    }
}
