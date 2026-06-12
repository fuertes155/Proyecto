package com.cooperativa.fintech.domain.identity.port;

import com.cooperativa.fintech.domain.identity.model.ComplianceListType;
import com.cooperativa.fintech.domain.identity.model.ComplianceResult;

import java.util.UUID;

public interface ComplianceCheckPort {

    ComplianceResult checkUser(UUID userId, ComplianceListType listType);

    void persistCheck(UUID userId, ComplianceListType listType, ComplianceResult result, String details);
}
