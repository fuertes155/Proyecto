package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.BiometricRegistrationRequest;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.BiometricRegistration;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.domain.identity.model.ComplianceResult;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.BiometricRegistrationPort;
import com.cooperativa.met.domain.identity.port.ComplianceCheckPort;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterBiometricUseCase {

    private static final BigDecimal MIN_LIVENESS_SCORE = new BigDecimal("0.8500");

    private final UserRepositoryPort userRepository;
    private final BiometricRegistrationPort biometricRegistrationPort;
    private final ComplianceCheckPort complianceCheckPort;
    private final EncryptionPort encryptionPort;

    @Transactional
    public void execute(BiometricRegistrationRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        BigDecimal livenessScore = simulateLivenessCheck(request.selfieImageBase64());
        if (livenessScore.compareTo(MIN_LIVENESS_SCORE) < 0) {
            throw new BusinessRuleException("LIVENESS_FAILED", "Prueba de vida no superada");
        }

        for (ComplianceListType listType : ComplianceListType.values()) {
            ComplianceResult result = complianceCheckPort.checkUser(user.getId(), listType);
            complianceCheckPort.persistCheck(user.getId(), listType, result, null);
            if (result == ComplianceResult.MATCH) {
                userRepository.save(user.withStatus(UserStatus.BLOCKED).withKycStatus(KycStatus.REJECTED));
                throw new BusinessRuleException("COMPLIANCE_MATCH", "Usuario en lista restrictiva: " + listType);
            }
        }

        BiometricRegistration registration = BiometricRegistration.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .documentImage(encryptionPort.encrypt(request.documentImageBase64()))
                .selfieImage(encryptionPort.encrypt(request.selfieImageBase64()))
                .verified(false)
                .createdAt(Instant.now())
                .build()
                .markVerified(livenessScore);

        biometricRegistrationPort.save(registration);

        userRepository.save(user
                .withKycStatus(KycStatus.APPROVED)
                .withStatus(UserStatus.ACTIVE));
    }

    private BigDecimal simulateLivenessCheck(String selfieBase64) {
        return selfieBase64 != null && !selfieBase64.isBlank()
                ? new BigDecimal("0.9500")
                : BigDecimal.ZERO;
    }
}
