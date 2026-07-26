package com.cooperativa.met.domain.support.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    @Value("${met.ai.gemini.api-key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public String generateReply(String userMessage) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.contains("tu_api_key")) {
            log.warn("Gemini API key no está configurada, devolviendo mock");
            return "¡Hola! Soy tu asistente 24/7 (Mock). No tengo una API Key configurada todavía. Me dijiste: " + userMessage;
        }

        try {
            String url = GEMINI_API_URL + geminiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String prompt = "Actúa como un asesor financiero amigable de una cooperativa llamada MET Platform. REGLA CRÍTICA: Responde SIEMPRE de forma MUY corta y concisa (máximo 2 oraciones). NO uses ningún formato markdown (NADA de asteriscos, negritas ni viñetas). El usuario te dice: " + userMessage;
            
            java.util.Map<String, Object> requestMap = java.util.Map.of(
                "contents", java.util.List.of(
                    java.util.Map.of(
                        "parts", java.util.List.of(
                            java.util.Map.of("text", prompt)
                        )
                    )
                )
            );

            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(requestMap);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getBody() != null && response.getBody().containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    return (String) parts.get(0).get("text");
                }
            }
            return "Lo siento, no pude procesar la respuesta en este momento.";
        } catch (Exception e) {
            log.error("Error al comunicarse con Gemini", e);
            return "Ha ocurrido un error de conexión con mi cerebro artificial. Por favor, intenta de nuevo.";
        }
    }
}
