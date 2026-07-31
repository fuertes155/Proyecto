package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.application.compliance.dto.RestrictiveListMatchResponse;
import com.cooperativa.met.domain.identity.model.ComplianceCheckRecord;
import com.cooperativa.met.domain.identity.model.ComplianceResult;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.ComplianceCheckPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Usuarios cuyo nombre coincidió con alguna lista restrictiva (OFAC/ONU) durante el KYC o la solicitud de crédito. */
@Service
@RequiredArgsConstructor
public class ListRestrictiveListMatchesUseCase {

    private final ComplianceCheckPort complianceCheckPort;
    private final UserRepositoryPort userRepository;

    public List<RestrictiveListMatchResponse> execute(int page, int pageSize) {
        List<ComplianceCheckRecord> matches = complianceCheckPort.findByResult(ComplianceResult.MATCH, page, pageSize);
        return matches.stream().map(record -> {
            User user = userRepository.findById(record.getUserId()).orElse(null);
            String fullName = user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : "Usuario eliminado";
            String documentNumber = user != null ? user.getDocumentNumber() : "-";
            return RestrictiveListMatchResponse.from(record, fullName, documentNumber);
        }).toList();
    }
}
