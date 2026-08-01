package com.cooperativa.met.infrastructure.security;

import com.cooperativa.met.application.account.dto.TransferRequest;
import com.cooperativa.met.application.account.usecase.ExecuteTransferUseCase;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.cooperativa.met.TestRedisConfig.class)
@AutoConfigureMockMvc
class SecurityXssIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MetSecurityProperties securityProperties;

    @MockBean
    private ExecuteTransferUseCase executeTransferUseCase;

    @MockBean
    private DeviceAttestationService attestationService;

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = {"USER"}) // ID de usuario mockeado
    void shouldSanitizeXssPayloadInTransferConcept() throws Exception {
        // Arrange
        Mockito.when(attestationService.verifyIntegrity(Mockito.any(), Mockito.any())).thenReturn(true);
        String maliciousConcept = "Regalo <script>alert('hack')</script>";
        String safeConcept = "Regalo &lt;script&gt;alert(&#39;hack&#39;)&lt;/script&gt;";
        
        // Creamos el JSON con el ataque XSS crudo
        String jsonPayload = """
                {
                    "destinationAccountId": "550e8400-e29b-41d4-a716-446655440000",
                    "amount": 500.00,
                    "concept": "%s",
                    "pin": "1234",
                    "otp": "000000",
                    "idempotencyKey": "key-123"
                }
                """.formatted(maliciousConcept);

        // Act
        String timestamp = String.valueOf(System.currentTimeMillis());
        mockMvc.perform(post("/v1/accounts/transactions/transfer")
                .servletPath("/v1/accounts/transactions/transfer")
                .header("X-Signature", sign("POST", "/v1/accounts/transactions/transfer", timestamp, jsonPayload))
                .header("X-Timestamp", timestamp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk());

        // Assert: Capturamos el objeto java (TransferRequest) que Spring construyó a partir del JSON
        ArgumentCaptor<TransferRequest> captor = ArgumentCaptor.forClass(TransferRequest.class);
        Mockito.verify(executeTransferUseCase).execute(Mockito.any(UUID.class), captor.capture(), Mockito.anyString());

        TransferRequest capturedRequest = captor.getValue();
        
        // Verificamos que el Sanitizador Global de Jackson haya interceptado y limpiado el texto
        assertEquals(safeConcept, capturedRequest.concept(), "El ataque XSS debió ser neutralizado por el sanitizador.");
    }

    private String sign(String method, String path, String timestamp, String body) {
        String secret = securityProperties.getEncryption().getHmacSecret();
        if (secret == null || secret.isBlank()) {
            secret = securityProperties.getEncryption().getAesKey();
        }
        return HmacSigner.sign(method, path, timestamp, body, secret);
    }
}
