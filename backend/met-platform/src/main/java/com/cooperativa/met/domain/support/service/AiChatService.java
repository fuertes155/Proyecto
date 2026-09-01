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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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

    // Timeouts explícitos: sin esto RestTemplate espera indefinidamente y una
    // caída/lentitud de Gemini deja la petición de chat colgada minutos.
    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(20000);
        return new RestTemplate(factory);
    }

    // "gemini-flash-latest" (alias) venía devolviendo 503 "high demand" de forma
    // sostenida. gemini-2.5-flash-lite está pensado para alto throughput / baja
    // latencia y tiene mejor disponibilidad. Ante 503/429 se reintenta una vez
    // con gemini-2.5-flash como respaldo (ver generateReply).
    private static final String GEMINI_MODEL = "gemini-2.5-flash-lite";
    private static final String GEMINI_FALLBACK_MODEL = "gemini-flash-lite-latest";
    private static final String GEMINI_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=";

    public String generateReply(String userMessage) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.contains("tu_api_key")) {
            log.warn("Gemini API key no está configurada, devolviendo mock");
            return "¡Hola! Soy tu asistente 24/7 (Mock). No tengo una API Key configurada todavía. Me dijiste: " + userMessage;
        }

        String prompt = "Actúa como un asesor financiero amigable de una cooperativa llamada MET Platform. REGLA CRÍTICA: Responde SIEMPRE de forma MUY corta y concisa (máximo 2 oraciones). NO uses ningún formato markdown (NADA de asteriscos, negritas ni viñetas). El usuario te dice: " + userMessage;

        // Modelo principal; si da 503/429 (saturación), un intento con el de respaldo.
        String reply = callGemini(GEMINI_MODEL, prompt);
        if (reply == null) {
            reply = callGemini(GEMINI_FALLBACK_MODEL, prompt);
        }
        return reply != null ? reply
                : "El asistente está ocupado en este momento. Intenta de nuevo en unos segundos.";
    }

    /** Devuelve la respuesta de Gemini, o null si falló de forma reintentable. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private String callGemini(String model, String prompt) {
        try {
            String url = String.format(GEMINI_URL_TEMPLATE, model) + geminiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestMap = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
            String requestBody = new ObjectMapper().writeValueAsString(requestMap);

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
            log.warn("Gemini ({}) respondió sin 'candidates'", model);
            return null;
        } catch (HttpServerErrorException e) {
            // 503 "high demand", 500, etc. — reintentable con el modelo de respaldo.
            log.warn("Gemini ({}) no disponible: {}", model, e.getStatusCode());
            return null;
        } catch (HttpClientErrorException e) {
            // 400/401/403 → problema de key o request; no tiene sentido reintentar.
            log.error("Gemini ({}) rechazó la petición: {} {}", model, e.getStatusCode(), e.getResponseBodyAsString());
            return "No pude responder ahora mismo. Un asesor te contactará pronto.";
        } catch (Exception e) {
            log.error("Error al comunicarse con Gemini ({})", model, e);
            return null;
        }
    }
}
