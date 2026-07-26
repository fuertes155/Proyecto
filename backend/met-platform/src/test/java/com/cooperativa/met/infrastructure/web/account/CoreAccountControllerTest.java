package com.cooperativa.met.infrastructure.web.account;

import com.cooperativa.met.application.account.dto.CoreAccountResponse;
import com.cooperativa.met.application.account.dto.TransferRequest;
import com.cooperativa.met.application.account.usecase.ExecuteTransferUseCase;
import com.cooperativa.met.application.account.usecase.GetMyAccountUseCase;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
@org.springframework.context.annotation.Import(com.cooperativa.met.TestRedisConfig.class)
@AutoConfigureMockMvc
class CoreAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetMyAccountUseCase getMyAccountUseCase;

    @MockBean
    private ExecuteTransferUseCase executeTransferUseCase;

    @Test
    @WithMockUser // Simula un usuario autenticado para que Spring Security deje pasar la petición
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
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("100020003000"))
                .andExpect(jsonPath("$.principalBalance").value(1500.50))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestWhenTransferFailsBusinessRule() throws Exception {
        // Arrange
        Mockito.doThrow(new BusinessRuleException("INSUFFICIENT_FUNDS", "Fondos insuficientes"))
                .when(executeTransferUseCase).execute(any(), any(TransferRequest.class), any());

        // Act & Assert
        mockMvc.perform(post("/v1/accounts/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationAccount\": \"999\", \"amount\": 5000, \"description\": \"pago\", \"otpCode\": \"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Fondos insuficientes"));
    }
}
