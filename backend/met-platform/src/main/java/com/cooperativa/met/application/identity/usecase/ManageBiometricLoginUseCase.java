package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.EnableBiometricLoginRequest;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.audit.AuditLogService;
import com.cooperativa.met.infrastructure.security.PinAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Activa o desactiva el ingreso con huella. El login la valida comparando el hash de la llave que
 * envía el celular con {@code User.biometricHash} (ver {@link LoginUseCase}).
 */
@Service
@RequiredArgsConstructor
public class ManageBiometricLoginUseCase {

    private final UserRepositoryPort userRepository;
    private final EncryptionPort encryptionPort;
    private final PinAttemptService pinAttemptService;
    private final AuditLogService auditLogService;

    @Transactional
    public void enable(UUID userId, EnableBiometricLoginRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        pinAttemptService.checkBlocked(userId);

        String plainPin;
        try {
            plainPin = encryptionPort.decryptRsa(request.pin());
        } catch (Exception e) {
            throw new BusinessRuleException("SECURITY_ERROR", "Error de seguridad en la encriptación de extremo a extremo");
        }

        if (!encryptionPort.verifyPin(plainPin, user.getPinHash())) {
            auditLogService.logFailure(userId, AuditLogService.BIOMETRIC_LOGIN_ENABLED,
                    "USER", userId.toString(), "{\"reason\":\"INVALID_PIN\"}");
            pinAttemptService.checkAndRecordFailure(userId); // lanza INVALID_PIN o ACCOUNT_LOCKED
            throw new BusinessRuleException("INVALID_PIN", "El PIN es incorrecto");
        }
        pinAttemptService.resetAttempts(userId);

        // Una sola huella activa por socio: activarla en otro celular reemplaza la anterior.
        userRepository.save(user.withBiometricHash(encryptionPort.hashBiometric(request.biometricToken())));

        auditLogService.logSuccess(userId, AuditLogService.BIOMETRIC_LOGIN_ENABLED,
                "USER", userId.toString(), null);
    }

    @Transactional
    public void disable(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (user.getBiometricHash() == null) return;

        userRepository.save(user.withBiometricHash(null));

        auditLogService.logSuccess(userId, AuditLogService.BIOMETRIC_LOGIN_DISABLED,
                "USER", userId.toString(), null);
    }
}
