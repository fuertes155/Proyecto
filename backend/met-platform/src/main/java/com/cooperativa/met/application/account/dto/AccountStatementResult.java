package com.cooperativa.met.application.account.dto;

public record AccountStatementResult(
        String fileName,
        byte[] content
) {
}
