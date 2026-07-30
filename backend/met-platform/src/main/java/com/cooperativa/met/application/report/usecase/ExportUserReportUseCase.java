package com.cooperativa.met.application.report.usecase;

import com.cooperativa.met.application.report.dto.ReportFileResult;
import com.cooperativa.met.application.report.dto.UserReportData;
import com.cooperativa.met.application.report.service.ExcelReportRenderer;
import com.cooperativa.met.application.report.service.PdfReportRenderer;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExportUserReportUseCase {

    private final GenerateUserReportDataUseCase generateUserReportDataUseCase;
    private final PdfReportRenderer pdfReportRenderer;
    private final ExcelReportRenderer excelReportRenderer;

    public ReportFileResult execute(UUID userId, LocalDate from, LocalDate to, String format) {
        UserReportData data = generateUserReportDataUseCase.execute(userId, from, to);
        String fileBase = "reporte_" + data.accountNumber() + "_" + from + "_a_" + to;

        return switch (format == null ? "pdf" : format.toLowerCase()) {
            case "pdf" -> new ReportFileResult(fileBase + ".pdf", pdfReportRenderer.render(data), "application/pdf");
            case "xlsx", "excel" -> new ReportFileResult(fileBase + ".xlsx", excelReportRenderer.render(data),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            default -> throw new BusinessRuleException("INVALID_FORMAT", "Formato no soportado, usa 'pdf' o 'xlsx'");
        };
    }
}
