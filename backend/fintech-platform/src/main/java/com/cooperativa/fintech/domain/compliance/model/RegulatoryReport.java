package com.cooperativa.fintech.domain.compliance.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class RegulatoryReport {

    private final UUID id;
    private final SupersolidariaReportType reportType;
    private final int periodYear;
    private final int periodMonth;
    private final String entityCode;
    private final ReportStatus status;
    private final String fileName;
    private final String filePath;
    private final Long fileSizeBytes;
    private final int recordCount;
    private final String checksumSha256;
    private final UUID generatedBy;
    private final String errorMessage;
    private final Instant generatedAt;
    private final Instant createdAt;

    public RegulatoryReport markGenerating() {
        return toBuilder().status(ReportStatus.GENERATING).build();
    }

    public RegulatoryReport markCompleted(String fileName, String filePath, long fileSize,
                                          int recordCount, String checksum) {
        return toBuilder()
                .status(ReportStatus.COMPLETED)
                .fileName(fileName)
                .filePath(filePath)
                .fileSizeBytes(fileSize)
                .recordCount(recordCount)
                .checksumSha256(checksum)
                .generatedAt(Instant.now())
                .build();
    }

    public RegulatoryReport markFailed(String error) {
        return toBuilder()
                .status(ReportStatus.FAILED)
                .errorMessage(error)
                .generatedAt(Instant.now())
                .build();
    }
}
