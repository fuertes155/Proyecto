package com.cooperativa.met.infrastructure.security;

import com.cooperativa.met.infrastructure.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de rate limiting para los endpoints de autenticación.
 *
 * <p>Usa el algoritmo Token Bucket (Bucket4j) por IP. Cada IP obtiene una
 * capacidad de {@code maxRequests} tokens que se recargan gradualmente a
 * razón de {@code maxRequests} cada {@code windowSeconds} segundos.</p>
 *
 * <p>Endpoints protegidos:
 * <ul>
 *   <li>POST /v1/auth/login</li>
 *   <li>POST /v1/auth/biometric</li>
 *   <li>POST /v1/auth/register</li>
 * </ul>
 * </p>
 */
@Slf4j
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String[] PROTECTED_PATHS = {
            "/v1/auth/login",
            "/v1/auth/biometric",
            "/v1/auth/register"
    };

    private final RateLimitProperties props;
    private final ObjectMapper objectMapper;
    // Cache de buckets por IP — se limpia automáticamente cuando la JVM tenga poca memoria
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(RateLimitProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!props.isEnabled() || !isProtectedPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        Bucket bucket = bucketCache.computeIfAbsent(clientIp, this::newBucket);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit excedido para IP: {} en ruta: {}", clientIp, request.getRequestURI());
            sendRateLimitResponse(response);
        }
    }

    /**
     * Determina si la solicitud va hacia alguno de los endpoints protegidos.
     */
    private boolean isProtectedPath(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        for (String path : PROTECTED_PATHS) {
            if (uri.endsWith(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Crea un nuevo bucket con la configuración de propiedades.
     * Usa Refill greedy para recargar tokens gradualmente (no en ráfaga).
     */
    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(
                props.getMaxRequests(),
                Refill.greedy(props.getMaxRequests(), Duration.ofSeconds(props.getWindowSeconds()))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Extrae la IP real del cliente, considerando proxies y balanceadores de carga.
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Tomar solo la primera IP (la original del cliente)
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Envía la respuesta HTTP 429 con un cuerpo JSON estándar.
     */
    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // Indica cuándo puede volver a intentar (en segundos)
        response.setHeader("Retry-After", String.valueOf(props.getWindowSeconds()));

        Map<String, Object> body = Map.of(
                "code", "RATE_LIMIT_EXCEEDED",
                "message", "Demasiados intentos. Por favor espera " +
                           props.getWindowSeconds() + " segundos antes de intentar de nuevo.",
                "timestamp", Instant.now().toString()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
