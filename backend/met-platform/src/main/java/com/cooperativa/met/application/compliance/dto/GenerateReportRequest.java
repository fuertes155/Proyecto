package com.cooperativa.met.application.compliance.dto;

import com.cooperativa.met.domain.compliance.model.SupersolidariaReportType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GenerateReportRequest(
        @NotNull SupersolidariaReportType reportType,
        @Min(2020) @Max(2100) Integer periodYear,
        @Min(1) @Max(12) Integer periodMonth
) {
}
