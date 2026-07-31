package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.compliance.service.ComplianceBlockService;
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
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.identity.port.ComplianceCheckPort;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterBiometricUseCase {

    private static final BigDecimal MIN_LIVENESS_SCORE = new BigDecimal("0.8500");

    // Firmas binarias (magic bytes) de los formatos de foto que aceptamos.
    // Cualquier otra cosa (PDF, DOCX, etc.) se rechaza aunque el cliente la
    // haya dejado pasar como "imagen".
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};

    private final UserRepositoryPort userRepository;
    private final BiometricRegistrationPort biometricRegistrationPort;
    private final ComplianceCheckPort complianceCheckPort;
    private final ComplianceBlockService complianceBlockService;
    private final EncryptionPort encryptionPort;
    private final CoreAccountRepositoryPort coreAccountRepository;

    @Transactional
    public void execute(UUID userId, BiometricRegistrationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (user.getKycStatus() == KycStatus.APPROVED) {
            throw new BusinessRuleException("KYC_ALREADY_APPROVED", "El usuario ya ha verificado su identidad");
        }

        validatePhoto(request.documentImageBase64(), "La foto de la cédula (frente)");
        validatePhoto(request.documentBackImageBase64(), "La foto de la cédula (reverso)");
        validatePhoto(request.selfieImageBase64(), "La selfie");
        validatePhoto(request.signatureImageBase64(), "La firma digital");

        BigDecimal livenessScore = simulateLivenessCheck(request.selfieImageBase64());
        if (livenessScore.compareTo(MIN_LIVENESS_SCORE) < 0) {
            throw new BusinessRuleException("LIVENESS_FAILED", "Prueba de vida no superada");
        }

        for (ComplianceListType listType : ComplianceListType.values()) {
            ComplianceResult result = complianceCheckPort.checkUser(user.getId(), listType);
            complianceCheckPort.persistCheck(user.getId(), listType, result, null);
            if (result == ComplianceResult.MATCH) {
                // En su propia transacción (REQUIRES_NEW): la excepción de abajo revierte
                // esta transacción, y ese bloqueo no puede depender de que ella confirme.
                complianceBlockService.blockUser(user.getId());
                throw new BusinessRuleException("COMPLIANCE_MATCH", "Usuario en lista restrictiva: " + listType);
            }
        }

        BiometricRegistration registration = BiometricRegistration.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .documentImage(encryptionPort.encrypt(request.documentImageBase64()))
                .documentBackImage(encryptionPort.encrypt(request.documentBackImageBase64()))
                .selfieImage(encryptionPort.encrypt(request.selfieImageBase64()))
                .signatureImage(encryptionPort.encrypt(request.signatureImageBase64()))
                .verified(false)
                .createdAt(Instant.now())
                .build()
                .markVerified(livenessScore);

        biometricRegistrationPort.save(registration);

        userRepository.save(user
                .withKycStatus(KycStatus.APPROVED)
                .withStatus(UserStatus.ACTIVE));
                
        // 🛡️ Create CoreAccount for the user so they can operate financially
        String accountNumber = "10" + String.format("%08d", new java.security.SecureRandom().nextInt(100000000));
        CoreAccount account = CoreAccount.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .accountNumber(accountNumber)
                .principalBalance(BigDecimal.ZERO)
                .interestBalance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        coreAccountRepository.save(account);
    }

    private BigDecimal simulateLivenessCheck(String selfieBase64) {
        return selfieBase64 != null && !selfieBase64.isBlank()
                ? new BigDecimal("0.9500")
                : BigDecimal.ZERO;
    }

    private void validatePhoto(String base64Image, String fieldLabel) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Image);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("INVALID_IMAGE_FORMAT", fieldLabel + " no es una imagen válida.");
        }

        if (!startsWith(bytes, JPEG_MAGIC) && !startsWith(bytes, PNG_MAGIC)) {
            throw new BusinessRuleException("INVALID_IMAGE_FORMAT",
                    fieldLabel + " debe ser una foto en formato JPG o PNG. No se permiten archivos PDF u otros documentos.");
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
