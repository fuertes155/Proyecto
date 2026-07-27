package com.cooperativa.met.infrastructure.web.account;

import com.cooperativa.met.application.account.dto.CoreAccountResponse;
import com.cooperativa.met.application.account.dto.TransferRequest;
import com.cooperativa.met.application.account.usecase.ExecuteTransferUseCase;
import com.cooperativa.met.application.account.usecase.GetMyAccountUseCase;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.infrastructure.security.DeviceAttestationService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.cooperativa.met.TestRedisConfig.class)
@AutoConfigureMockMvc
class CoreAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetMyAccountUseCase getMyAccountUseCase;

    @MockBean
    private ExecuteTransferUseCase executeTransferUseCase;

    @MockBean
    private DeviceAttestationService deviceAttestationService;

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldReturnMyAccountDetails() throws Exception {
        // Arrange
        CoreAccountResponse mockResponse = new CoreAccountResponse(
                UUID.randomUUID(),
                "100020003000",
                new BigDecimal("1500.50"),
                BigDecimal.ZERO,
                "ACTIVE"
        );
        Mockito.when(getMyAccountUseCase.execute(any())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/v1/accounts/me")
                .header("X-Signature", "test-skip-hmac")
                .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("100020003000"))
                .andExpect(jsonPath("$.principalBalance").value(1500.50))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldReturnBadRequestWhenTransferFailsBusinessRule() throws Exception {
        // Arrange
        Mockito.when(deviceAttestationService.verifyIntegrity(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doThrow(new BusinessRuleException("INSUFFICIENT_FUNDS", "Fondos insuficientes"))
                .when(executeTransferUseCase).execute(any(), any(TransferRequest.class), any());

        String validJson = "{\"destinationAccountId\": \"550e8400-e29b-41d4-a716-446655440000\", \"amount\": 5000.00, \"concept\": \"pago\", \"pin\": \"1234\", \"otp\": \"123456\", \"idempotencyKey\": \"test-key\"}";

        // Act & Assert
        mockMvc.perform(post("/v1/accounts/transactions/transfer")
                .header("X-Signature", "test-skip-hmac")
                .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Fondos insuficientes"));
    }
}
