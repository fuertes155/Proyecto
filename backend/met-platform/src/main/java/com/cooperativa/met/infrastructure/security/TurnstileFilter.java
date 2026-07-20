package com.cooperativa.met.infrastructure.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Filtro que intercepta rutas publicas para validar el token de Cloudflare Turnstile.
 */
@Slf4j
public class TurnstileFilter extends OncePerRequestFilter {

    private static final String CAPTCHA_HEADER = "X-Captcha-Token";

    // Rutas protegidas por CAPTCHA
    private static final String[] PROTECTED_PATHS = {
        "/v1/auth/login",
        "/v1/auth/register",
        "/v1/auth/pin-recovery/request"
    };

    private final TurnstileValidationService turnstileValidationService;
    private final ObjectMapper objectMapper;

    public TurnstileFilter(TurnstileValidationService turnstileValidationService, ObjectMapper objectMapper) {
        this.turnstileValidationService = turnstileValidationService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isProtectedPath(request)) {
            String captchaToken = request.getHeader(CAPTCHA_HEADER);
            String clientIp = extractClientIp(request);

            boolean isValid = turnstileValidationService.validateCaptcha(captchaToken, clientIp);
            
            if (!isValid) {
                log.warn("Captcha invalido o ausente en ruta {}. Bloqueando peticion de IP: {}", request.getRequestURI(), clientIp);
                sendUnauthorizedResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProtectedPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String path : PROTECTED_PATHS) {
            if (uri.endsWith(path)) return true;
        }
        return false;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "code", "CAPTCHA_REQUIRED",
                "message", "Validacion de seguridad fallida. Por favor, recarga y vuelve a intentarlo.",
                "timestamp", Instant.now().toString()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
