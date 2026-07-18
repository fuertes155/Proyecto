package com.cooperativa.met.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filtro de seguridad extremo: Geo-Fencing y bloqueo de red.
 * Bloquea peticiones de rangos de IP no permitidos (ej. fuera del país o nodos Tor).
 */
@Slf4j
@Component
public class GeoBlockingFilter extends OncePerRequestFilter {

    // Simulación de IPs prohibidas o rangos internacionales (En prod usar MaxMind GeoIP o similar)
    private static final List<String> BLACKLISTED_IPS = List.of(
            "198.51.100.", // Tor exit node simulation
            "203.0.113."   // Foreign IP block simulation
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = extractIp(request);
        
        // Excluimos rutas locales (actuator) del geo-bloqueo
        if (request.getRequestURI().startsWith("/actuator") || "127.0.0.1".equals(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isBlocked = BLACKLISTED_IPS.stream().anyMatch(clientIp::startsWith);

        if (isBlocked) {
            log.warn("🚨 [GEO-BLOCK] Conexión rechazada por geocerca. IP maliciosa o extranjera: {}", clientIp);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"GEO_BLOCKED\", \"message\": \"Access from your region or network is strictly prohibited.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractIp(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .map(ip -> ip.split(",")[0].trim())
                .orElse(request.getRemoteAddr());
    }
}
