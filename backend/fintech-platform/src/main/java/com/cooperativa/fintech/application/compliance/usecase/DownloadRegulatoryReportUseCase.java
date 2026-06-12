package com.cooperativa.fintech.application.compliance.usecase;

import com.cooperativa.fintech.domain.common.exception.BusinessRuleException;
import com.cooperativa.fintech.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.fintech.domain.compliance.model.RegulatoryReport;
import com.cooperativa.fintech.domain.compliance.model.ReportStatus;
import com.cooperativa.fintech.domain.compliance.port.RegulatoryReportPort;
import com.cooperativa.fintech.domain.compliance.port.ReportFileStoragePort;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DownloadRegulatoryReportUseCase {

    private final RegulatoryReportPort reportPort;
    private final ReportFileStoragePort storagePort;

    @Transactional(readOnly = true)
    public DownloadResult execute(UUID reportId) {
        RegulatoryReport report = reportPort.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado"));

        if (report.getStatus() != ReportStatus.COMPLETED || report.getFilePath() == null) {
            throw new BusinessRuleException("REPORT_NOT_READY", "El reporte no está disponible para descarga");
        }

        byte[] content = storagePort.read(report.getFilePath());
        return DownloadResult.builder()
                .fileName(report.getFileName())
                .content(content)
                .checksumSha256(report.getChecksumSha256())
                .build();
    }

    @Getter
    @Builder
    public static class DownloadResult {
        private final String fileName;
        private final byte[] content;
        private final String checksumSha256;
    }
}
