package com.cooperativa.met.infrastructure.security;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.cooperativa.met.infrastructure.config.TurnstileProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TurnstileValidationService {

    private static final String TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private final TurnstileProperties properties;
    private final RestTemplate restTemplate;

    public TurnstileValidationService(TurnstileProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }

    public boolean validateCaptcha(String token, String remoteIp) {
        if (!properties.isEnabled()) {
            return true;
        }
        
        if (token == null || token.isBlank()) {
            log.warn("Token de Turnstile faltante.");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("secret", properties.getSecretKey());
            map.add("response", token);
            if (remoteIp != null && !remoteIp.isBlank()) {
                map.add("remoteip", remoteIp);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            
            ParameterizedTypeReference<Map<String, Object>> responseType = 
                new ParameterizedTypeReference<Map<String, Object>>() {};
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                TURNSTILE_VERIFY_URL, 
                HttpMethod.POST, 
                request, 
                responseType
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                Boolean success = (Boolean) responseBody.get("success");
                if (Boolean.TRUE.equals(success)) {
                    return true;
                } else {
                    log.warn("Fallo validacion Turnstile: {}", responseBody);
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error al validar el token de Turnstile", e);
            // Dependiendo de la política de seguridad, si falla la API de Cloudflare,
            // podemos devolver false (seguro) o true (disponibilidad).
            // Por seguridad financiera, fallamos cerrado.
            return false;
        }
    }
}
