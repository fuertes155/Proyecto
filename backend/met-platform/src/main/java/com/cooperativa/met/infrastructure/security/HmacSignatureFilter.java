package com.cooperativa.met.infrastructure.security;

import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@org.springframework.boot.context.properties.EnableConfigurationProperties(MetSecurityProperties.class)
public class HmacSignatureFilter extends OncePerRequestFilter {

    private static final String HEADER_SIGNATURE = "X-Signature";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final long MAX_TIME_DIFF_SECONDS = 60; // 60 seconds tolerance

    private final MetSecurityProperties securityProperties;

    // Métodos que requieren firma
    private static final List<String> PROTECTED_METHODS = List.of(
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name()
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();

        // 1. Skip si no es un método protegido o si es preflight CORS
        if (!PROTECTED_METHODS.contains(method) || method.equalsIgnoreCase(HttpMethod.OPTIONS.name())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip Swagger o Webhooks o Auth que no tienen nuestra lógica de app o no requieren firma
        // NOTA: se usa getServletPath() (sin el context-path "/api") en vez de getRequestURI(),
        // porque el cliente Dio firma el path SIN "/api" (su baseUrl ya lo incluye). Usar
        // getRequestURI() aquí rompía tanto este skip-list (nunca matcheaba "/v1/auth") como
        // la comparación de firma para el resto de endpoints protegidos.
        //
        // /v1/admin/** completo (no solo /v1/admin/auth) queda exento: el panel admin web ya
        // se protege con JWT + rol (ADMIN/SUPER_ADMIN) vía Spring Security, que es el control
        // correcto para un cliente web. La firma HMAC con secreto compartido es para la app
        // móvil (binario compilado); embeber ese mismo secreto en el bundle del panel web lo
        // dejaría visible para cualquiera que inspeccione el código — no aporta seguridad real.
        String path = request.getServletPath();
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.startsWith("/v1/webhooks") || path.startsWith("/v1/auth") || path.startsWith("/v1/admin")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extraer headers
        String signatureHeader = request.getHeader(HEADER_SIGNATURE);
        String timestampHeader = request.getHeader(HEADER_TIMESTAMP);

        if (signatureHeader == null || timestampHeader == null) {
            log.warn("Faltan headers de seguridad HMAC en {}", path);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Security Headers");
            return;
        }

        // 3. Validar Timestamp (Prevención de ataques de replay)
        try {
            long requestTime = Long.parseLong(timestampHeader);
            long now = Instant.now().toEpochMilli();
            long diffSeconds = Math.abs((now - requestTime) / 1000);
            
            if (diffSeconds > MAX_TIME_DIFF_SECONDS) {
                log.warn("Replay attack detectado o reloj desincronizado. Diff: {}s", diffSeconds);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Request Expired");
                return;
            }
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Timestamp");
            return;
        }

        // 4. Cachear el body para poder leerlo y luego pasarlo al controlador
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);

        String body = new String(cachedRequest.getCachedBody(), StandardCharsets.UTF_8);

        // 5. Calcular firma esperada
        // Formato: Method + Path + Timestamp + Body
        String expectedSignature = HmacSigner.sign(method, path, timestampHeader, body, resolveSecret());

        // 6. Comparar firmas
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), signatureHeader.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Invalid HMAC signature para {}", path);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Signature");
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }

    private String resolveSecret() {
        String secret = securityProperties.getEncryption().getHmacSecret();
        if (secret == null || secret.isBlank()) {
            // Fallback para dev local si no hay secreto, aunque Properties validator debería evitarlo
            secret = securityProperties.getEncryption().getAesKey();
        }
        return secret;
    }
}
