package com.cooperativa.fintech.application.compliance.dto;

import com.cooperativa.fintech.domain.compliance.model.ReportStatus;
import com.cooperativa.fintech.domain.compliance.model.SupersolidariaReportType;

import java.time.Instant;
import java.util.UUID;

public record RegulatoryReportResponse(
        UUID id,
        SupersolidariaReportType reportType,
        int periodYear,
        int periodMonth,
        String entityCode,
        ReportStatus status,
        String fileName,
        Long fileSizeBytes,
        int recordCount,
        String checksumSha256,
        Instant generatedAt,
        String downloadUrl
) {
}
