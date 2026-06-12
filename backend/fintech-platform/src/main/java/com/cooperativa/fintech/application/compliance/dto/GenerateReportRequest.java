package com.cooperativa.fintech.application.compliance.dto;

import com.cooperativa.fintech.domain.compliance.model.SupersolidariaReportType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GenerateReportRequest(
        @NotNull SupersolidariaReportType reportType,
        @Min(2020) @Max(2100) int periodYear,
        @Min(1) @Max(12) int periodMonth
) {
}
