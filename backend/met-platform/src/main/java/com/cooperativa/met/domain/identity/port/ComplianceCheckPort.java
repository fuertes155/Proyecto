package com.cooperativa.met.domain.identity.port;

import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.domain.identity.model.ComplianceResult;

import java.util.UUID;

public interface ComplianceCheckPort {

    ComplianceResult checkUser(UUID userId, ComplianceListType listType);

    void persistCheck(UUID userId, ComplianceListType listType, ComplianceResult result, String details);
}
