package com.cooperativa.met.application.compliance.dto;

import com.cooperativa.met.domain.compliance.model.ReportStatus;
import com.cooperativa.met.domain.compliance.model.SupersolidariaReportType;

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
