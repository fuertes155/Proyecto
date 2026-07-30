package com.cooperativa.met.infrastructure.web.report;

import com.cooperativa.met.application.report.dto.ReportFileResult;
import com.cooperativa.met.application.report.usecase.ExportUserReportUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ExportUserReportUseCase exportUserReportUseCase;

    private UUID getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UsernamePasswordAuthenticationToken jwtToken) {
            Object principal = jwtToken.getPrincipal();
            if (principal instanceof UUID uuid) {
                return uuid;
            } else if (principal instanceof String str) {
                return UUID.fromString(str);
            } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                return UUID.fromString(userDetails.getUsername());
            }
        }
        throw new IllegalStateException("No se pudo obtener el ID del usuario autenticado");
    }

    /**
     * Reporte consolidado del usuario: historial de movimientos, inversiones
     * activas, préstamos vigentes y rendimientos, para el rango [from, to].
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "pdf") String format) {
        ReportFileResult result = exportUserReportUseCase.execute(getAuthenticatedUserId(), from, to, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }
}
