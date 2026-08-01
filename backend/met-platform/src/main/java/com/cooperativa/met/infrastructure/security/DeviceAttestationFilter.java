package com.cooperativa.met.infrastructure.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

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
 * Filtro que exige pruebas criptográficas de la integridad del dispositivo (Play Integrity / App Attest)
 * en operaciones criticas (ej. transferencias, aplicar a prestamos, vinculacion de biometria).
 */
@Slf4j
public class DeviceAttestationFilter extends OncePerRequestFilter {

    private static final String ATTESTATION_HEADER = "X-Device-Attestation";
    private static final String PLATFORM_HEADER = "X-Platform"; // 'android' o 'ios'

    // Rutas + método protegidas de alto riesgo que requieren atestacion de hardware.
    // Deben coincidir EXACTO (método Y path) con los @PostMapping/@GetMapping reales de
    // los controladores — verificar aquí si se renombra algún endpoint crítico. Solo el
    // par método+path exacto listado exige atestación; p. ej. GET /v1/loans/applications
    // (listar) NO debe quedar atrapado por el POST al mismo path (aplicar a un préstamo).
    private static final Set<String> CRITICAL_METHOD_PATHS = Set.of(
        "POST /v1/auth/biometric",
        "POST /v1/loans/applications",
        "POST /v1/accounts/transactions/transfer",
        "GET /v1/accounts/verify"
    );

    private final DeviceAttestationService attestationService;
    private final ObjectMapper objectMapper;

    public DeviceAttestationFilter(DeviceAttestationService attestationService, ObjectMapper objectMapper) {
        this.attestationService = attestationService;
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

        if (isCriticalPath(request)) {
            String attestationToken = request.getHeader(ATTESTATION_HEADER);
            String platform = request.getHeader(PLATFORM_HEADER);
            
            if (platform == null || platform.isBlank()) {
                platform = "unknown";
            }

            boolean isDeviceValid = attestationService.verifyIntegrity(attestationToken, platform);
            
            if (!isDeviceValid) {
                log.warn("Fallo en la atestacion de dispositivo para la ruta critica {}. Plataforma: {}", request.getRequestURI(), platform);
                sendAttestationFailedResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isCriticalPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        for (String methodPath : CRITICAL_METHOD_PATHS) {
            int spaceIdx = methodPath.indexOf(' ');
            String requiredMethod = methodPath.substring(0, spaceIdx);
            String requiredPath = methodPath.substring(spaceIdx + 1);
            if (requiredMethod.equalsIgnoreCase(method) && uri.endsWith(requiredPath)) {
                return true;
            }
        }
        return false;
    }

    private void sendAttestationFailedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.PRECONDITION_FAILED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "code", "DEVICE_INTEGRITY_FAILED",
                "message", "El dispositivo no cumple con los requisitos de seguridad o la app no es oficial.",
                "timestamp", Instant.now().toString()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
