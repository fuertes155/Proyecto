package com.cooperativa.met.infrastructure.security;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cooperativa.met.infrastructure.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Filtro de rate limiting para los endpoints de autenticación.
 *
 * <p>Aplica contadores en Redis con TTL por ventana.</p>
 * <ul>
 *   <li>Por IP: ratelimit:auth:ip:{ip}</li>
 *   <li>Por usuario (si hay Authentication): ratelimit:auth:user:{userId}</li>
 * </ul>
 */
@Slf4j
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String[] PROTECTED_PATHS = {
            "/v1/auth/login",
            "/v1/auth/biometric",
            "/v1/auth/register",
            "/v1/auth/refresh"
    };

    /** Rutas de OTP: límite más estricto (3 intentos / 10 min) para evitar spam de correos */
    private static final String[] OTP_PATHS = {
            "/v1/auth/resend-email-otp",
            "/v1/auth/verify-email",
            "/v1/auth/pin-recovery/request"
    };

    private static final int OTP_MAX_ATTEMPTS = 3;
    private static final long OTP_WINDOW_SECONDS = 600; // 10 minutos

    // Fallbacks si por cualquier razón no podemos leer los campos de RateLimitProperties
    private static final boolean DEFAULT_ENABLED = true;
    private static final int DEFAULT_MAX_REQUESTS = 10;
    private static final long DEFAULT_WINDOW_SECONDS = 60;

    private final RateLimitProperties props;
    private final ObjectMapper objectMapper;
    private final RedisRateLimiter redisRateLimiter;

    public AuthRateLimitFilter(RateLimitProperties props, ObjectMapper objectMapper, RedisRateLimiter redisRateLimiter) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.redisRateLimiter = redisRateLimiter;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // IMPORTANTE: no interceptar preflight CORS.
        // El preflight es OPTIONS y Spring necesita poder agregar los headers CORS.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            // Asegurar que el preflight tenga headers CORS incluso si algún filtro/handler
            // corta la ejecución antes de que Spring los agregue.
            response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin") == null ? "*" : request.getHeader("Origin"));
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setStatus(HttpStatus.OK.value());
            return;
        }


        if (!isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isOtpPath = isOtpPath(request);
        boolean isAuthPath = !isOtpPath && isProtectedPath(request);

        if (!isOtpPath && !isAuthPath) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        boolean allowed;

        if (isOtpPath) {
            // Rutas OTP: límite estricto 3 intentos / 10 minutos
            allowed = redisRateLimiter.tryConsume(
                    "ratelimit:otp:ip:" + clientIp,
                    OTP_MAX_ATTEMPTS,
                    Duration.ofSeconds(OTP_WINDOW_SECONDS)
            );
        } else {
            // Rutas de autenticación general: límite configurable
            boolean ipAllowed = tryConsumeIp(clientIp);
            UUID userId = currentUserId();
            boolean userAllowed = userId == null || tryConsumeUser(userId);
            allowed = ipAllowed && userAllowed;
        }

        if (allowed) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit excedido. path={} ip={}", request.getRequestURI(), clientIp);
            sendRateLimitResponse(response, isOtpPath ? OTP_WINDOW_SECONDS : windowSeconds());
        }
    }

    private boolean isProtectedPath(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return false;
        String uri = request.getRequestURI();
        for (String path : PROTECTED_PATHS) {
            if (uri.endsWith(path)) return true;
        }
        return false;
    }

    private boolean isOtpPath(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return false;
        String uri = request.getRequestURI();
        for (String path : OTP_PATHS) {
            if (uri.endsWith(path)) return true;
        }
        return false;
    }

    private boolean tryConsumeIp(String ip) {
        String key = "ratelimit:auth:ip:" + ip;
        return redisRateLimiter.tryConsume(
                key,
                maxRequests(),
                Duration.ofSeconds(windowSeconds())
        );
    }

    private boolean tryConsumeUser(UUID userId) {
        String key = "ratelimit:auth:user:" + userId;
        return redisRateLimiter.tryConsume(
                key,
                maxRequests(),
                Duration.ofSeconds(windowSeconds())
        );
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        if (auth.getPrincipal() instanceof UUID uuid) return uuid;
        return null;
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

    private void sendRateLimitResponse(HttpServletResponse response, long waitSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(waitSeconds));

        String minutes = waitSeconds >= 60 ? (waitSeconds / 60) + " minutos" : waitSeconds + " segundos";
        Map<String, Object> body = Map.of(
                "code", "RATE_LIMIT_EXCEEDED",
                "message", "Demasiados intentos. Por favor espera " + minutes + " antes de intentar de nuevo.",
                "timestamp", Instant.now().toString()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }

    private boolean isEnabled() {
        return props != null && props.isEnabled();
    }

    private int maxRequests() {
        return props != null ? props.getMaxRequests() : DEFAULT_MAX_REQUESTS;
    }

    private long windowSeconds() {
        return props != null ? props.getWindowSeconds() : DEFAULT_WINDOW_SECONDS;
    }
}
