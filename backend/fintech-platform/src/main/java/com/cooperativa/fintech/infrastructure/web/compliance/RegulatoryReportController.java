package com.cooperativa.fintech.infrastructure.web.compliance;

import com.cooperativa.fintech.application.compliance.dto.GenerateReportRequest;
import com.cooperativa.fintech.application.compliance.dto.RegulatoryReportResponse;
import com.cooperativa.fintech.application.compliance.usecase.DownloadRegulatoryReportUseCase;
import com.cooperativa.fintech.application.compliance.usecase.GenerateSupersolidariaReportUseCase;
import com.cooperativa.fintech.application.compliance.usecase.GetRegulatoryReportUseCase;
import com.cooperativa.fintech.application.compliance.usecase.ListRegulatoryReportsUseCase;
import com.cooperativa.fintech.domain.compliance.model.SupersolidariaReportType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/compliance/reports")
@RequiredArgsConstructor
public class RegulatoryReportController {

    private final GenerateSupersolidariaReportUseCase generateUseCase;
    private final ListRegulatoryReportsUseCase listUseCase;
    private final GetRegulatoryReportUseCase getUseCase;
    private final DownloadRegulatoryReportUseCase downloadUseCase;

    @GetMapping("/types")
    public ResponseEntity<List<Map<String, String>>> listReportTypes() {
        List<Map<String, String>> types = Arrays.stream(SupersolidariaReportType.values())
                .map(t -> Map.of("code", t.name(), "description", describe(t)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(types);
    }

    @PostMapping("/generate")
    public ResponseEntity<RegulatoryReportResponse> generate(
            Authentication authentication,
            @Valid @RequestBody GenerateReportRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(generateUseCase.execute(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<RegulatoryReportResponse>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(listUseCase.execute(year, month));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<RegulatoryReportResponse> get(@PathVariable UUID reportId) {
        return ResponseEntity.ok(getUseCase.execute(reportId));
    }

    @GetMapping("/{reportId}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID reportId) {
        DownloadRegulatoryReportUseCase.DownloadResult result = downloadUseCase.execute(reportId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.getFileName() + "\"")
                .header("X-Checksum-SHA256", result.getChecksumSha256())
                .contentType(MediaType.TEXT_PLAIN)
                .body(result.getContent());
    }

    private String describe(SupersolidariaReportType type) {
        return switch (type) {
            case ASOCIADOS -> "Reporte de asociados vinculados";
            case AHORROS_PROGRAMADOS -> "Cartera de ahorro programado";
            case CREDITO_PERSONAL -> "Cartera de crédito personal";
            case AHORRO_SOLIDARIO -> "Fondos de ahorro solidario";
            case SARLAFT -> "Resumen verificaciones SARLAFT/OFAC/ONU";
        };
    }
}
