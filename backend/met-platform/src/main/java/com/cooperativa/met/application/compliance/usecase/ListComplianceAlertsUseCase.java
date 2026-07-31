package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.application.compliance.dto.ComplianceAlertResponse;
import com.cooperativa.met.domain.compliance.model.AlertStatus;
import com.cooperativa.met.domain.compliance.model.ComplianceAlert;
import com.cooperativa.met.domain.compliance.port.ComplianceAlertRepositoryPort;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListComplianceAlertsUseCase {

    private final ComplianceAlertRepositoryPort alertRepository;
    private final UserRepositoryPort userRepository;

    public List<ComplianceAlertResponse> execute(AlertStatus status, int page, int pageSize) {
        List<ComplianceAlert> alerts = alertRepository.findByStatus(status, page, pageSize);
        return alerts.stream().map(this::toResponse).toList();
    }

    public long countByStatus(AlertStatus status) {
        return alertRepository.countByStatus(status);
    }

    private ComplianceAlertResponse toResponse(ComplianceAlert alert) {
        User user = userRepository.findById(alert.getUserId()).orElse(null);
        String fullName = user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : "Usuario eliminado";
        String documentNumber = user != null ? user.getDocumentNumber() : "-";
        return ComplianceAlertResponse.from(alert, fullName, documentNumber);
    }
}
