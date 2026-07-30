package com.cooperativa.met.application.report.dto;

public record ReportFileResult(String fileName, byte[] content, String contentType) {
}
