package com.cooperativa.fintech.application.compliance.mapper;

import com.cooperativa.fintech.application.compliance.dto.RegulatoryReportResponse;
import com.cooperativa.fintech.domain.compliance.model.RegulatoryReport;
import org.springframework.stereotype.Component;

@Component
public class RegulatoryReportMapper {

    public RegulatoryReportResponse toResponse(RegulatoryReport report) {
        String downloadUrl = report.getStatus() == com.cooperativa.fintech.domain.compliance.model.ReportStatus.COMPLETED
                ? "/v1/compliance/reports/" + report.getId() + "/download"
                : null;
        return new RegulatoryReportResponse(
                report.getId(),
                report.getReportType(),
                report.getPeriodYear(),
                report.getPeriodMonth(),
                report.getEntityCode(),
                report.getStatus(),
                report.getFileName(),
                report.getFileSizeBytes(),
                report.getRecordCount(),
                report.getChecksumSha256(),
                report.getGeneratedAt(),
                downloadUrl
        );
    }
}
