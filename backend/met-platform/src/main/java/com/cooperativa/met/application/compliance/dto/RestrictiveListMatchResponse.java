package com.cooperativa.met.application.compliance.dto;

import com.cooperativa.met.domain.identity.model.ComplianceCheckRecord;

import java.time.Instant;
import java.util.UUID;

public record RestrictiveListMatchResponse(
        UUID checkId,
        UUID userId,
        String userFullName,
        String userDocumentNumber,
        String listType,
        Instant checkedAt,
        String details
) {
    public static RestrictiveListMatchResponse from(ComplianceCheckRecord record, String userFullName, String userDocumentNumber) {
        return new RestrictiveListMatchResponse(
                record.getId(),
                record.getUserId(),
                userFullName,
                userDocumentNumber,
                record.getListType().name(),
                record.getCheckedAt(),
                record.getDetails()
        );
    }
}
