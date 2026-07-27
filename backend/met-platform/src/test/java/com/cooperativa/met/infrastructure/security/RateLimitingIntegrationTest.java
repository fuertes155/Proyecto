package com.cooperativa.met.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.cooperativa.met.TestRedisConfig.class)
@AutoConfigureMockMvc
class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // Simulamos la operación de Redis que incrementa el contador de peticiones
        valueOperations = Mockito.mock(ValueOperations.class);
        Mockito.when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldBlockRequestsAfterTenAttemptsWithTooManyRequests() throws Exception {
        // Arrange: 
        // El RateLimiter llamará a increment() por cada petición que ingrese.
        // Simulamos que cuenta del 1 al 11.
        Mockito.when(valueOperations.increment(anyString()))
                .thenReturn(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);

        String jsonPayload = "{\"documentType\": \"CC\", \"documentNumber\": \"123456789\", \"pin\": \"1234\"}";

        // Act & Assert: Primeras 10 peticiones (NO deben recibir 429)
        for (int i = 0; i < 10; i++) {
            final int attempt = i + 1;
            mockMvc.perform(post("/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonPayload))
                    .andExpect(result -> {
                        int statusCode = result.getResponse().getStatus();
                        if (statusCode == 429) {
                            throw new AssertionError("La petición " + attempt + " fue bloqueada incorrectamente antes de llegar al límite.");
                        }
                    });
        }

        // Act & Assert: Petición #11
        // Esta petición es la que hace que el contador pase a 11, excediendo el límite configurado de 10.
        // Debe ser rechazada instantáneamente por el filtro de seguridad.
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isTooManyRequests());
    }
}
