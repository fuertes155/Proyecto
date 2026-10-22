package com.cooperativa.met.domain.support.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Asistente de chat sobre Gemini: modelo principal, respaldo ante saturación y respuestas de contingencia. */
class AiChatServiceTest {

    private static final String PRIMARY = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=test-key";
    private static final String FALLBACK = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent?key=test-key";

    private AiChatService chat;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        chat = new AiChatService();
        ReflectionTestUtils.setField(chat, "geminiApiKey", "test-key");
        server = MockRestServiceServer.bindTo((RestTemplate) ReflectionTestUtils.getField(chat, "restTemplate")).build();
    }

    @Test
    void shouldAnswerWithGeminiKeepingTheAdvisorInstructions() {
        server.expect(requestTo(PRIMARY))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value(org.hamcrest.Matchers.containsString("¿Cómo ahorro más?")))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value(org.hamcrest.Matchers.containsString("máximo 2 oraciones")))
                .andRespond(withSuccess(reply("Ahorra un monto fijo cada semana."), MediaType.APPLICATION_JSON));

        assertEquals("Ahorra un monto fijo cada semana.", chat.generateReply("¿Cómo ahorro más?"));
    }

    @Test
    void shouldFallBackToTheSecondModelWhenTheFirstIsSaturated() {
        server.expect(requestTo(PRIMARY)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(FALLBACK)).andRespond(withSuccess(reply("Respuesta de respaldo."), MediaType.APPLICATION_JSON));

        assertEquals("Respuesta de respaldo.", chat.generateReply("hola"));
        server.verify();
    }

    @Test
    void shouldAlsoFallBackWhenTheFirstModelIsRateLimited() {
        server.expect(requestTo(PRIMARY)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(FALLBACK)).andRespond(withSuccess(reply("Respuesta de respaldo."), MediaType.APPLICATION_JSON));

        assertEquals("Respuesta de respaldo.", chat.generateReply("hola"));
    }

    @Test
    void shouldAskToRetryWhenBothModelsAreDown() {
        server.expect(requestTo(PRIMARY)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(FALLBACK)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertTrue(chat.generateReply("hola").contains("ocupado"));
    }

    @Test
    void shouldNotRetryARejectedRequest() {
        server.expect(requestTo(PRIMARY)).andRespond(withStatus(HttpStatus.FORBIDDEN).body("API key invalid"));

        assertTrue(chat.generateReply("hola").contains("asesor"));
        server.verify();
    }

    @Test
    void shouldAnswerWithAMockWhenNoApiKeyIsConfigured() {
        ReflectionTestUtils.setField(chat, "geminiApiKey", "tu_api_key_aqui");

        assertTrue(chat.generateReply("hola").contains("Mock"));
    }

    private static String reply(String text) {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + text + "\"}]}}]}";
    }
}
