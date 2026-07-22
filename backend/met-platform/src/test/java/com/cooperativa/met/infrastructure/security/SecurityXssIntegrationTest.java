package com.cooperativa.met.infrastructure.security;

import com.cooperativa.met.application.account.dto.TransferRequest;
import com.cooperativa.met.application.account.usecase.ExecuteTransferUseCase;
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
@AutoConfigureMockMvc
class SecurityXssIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExecuteTransferUseCase executeTransferUseCase;

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = {"USER"}) // ID de usuario mockeado
    void shouldSanitizeXssPayloadInTransferConcept() throws Exception {
        // Arrange
        String maliciousConcept = "Regalo <script>alert('hack')</script>";
        String safeConcept = "Regalo &lt;script&gt;alert('hack')&lt;/script&gt;";
        
        // Creamos el JSON con el ataque XSS crudo
        String jsonPayload = """
                {
                    "destinationAccountId": "550e8400-e29b-41d4-a716-446655440000",
                    "amount": 500.00,
                    "concept": "%s",
                    "pin": "1234",
                    "otp": "000000"
                }
                """.formatted(maliciousConcept);

        // Act
        mockMvc.perform(post("/v1/accounts/transfers")
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
}
