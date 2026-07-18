package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.UpdatePinRequest;
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

@Service
@RequiredArgsConstructor
public class UpdatePinUseCase {

    private final UserRepositoryPort userRepository;
    private final EncryptionPort encryptionPort;
    private final AuditLogService auditLogService;
    private final PinAttemptService pinAttemptService;

    @Transactional
    public void execute(UUID userId, UpdatePinRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
                
        pinAttemptService.checkBlocked(userId);

        String plainCurrentPin;
        String plainNewPin;
        
        try {
            plainCurrentPin = encryptionPort.decryptRsa(request.getCurrentPin());
            plainNewPin = encryptionPort.decryptRsa(request.getNewPin());
        } catch (Exception e) {
            auditLogService.logFailure(userId, AuditLogService.PIN_CHANGED,
                    "USER", userId.toString(), "{\"reason\":\"SECURITY_ERROR_RSA\"}");
            throw new BusinessRuleException("SECURITY_ERROR", "Error de seguridad en la encriptación de extremo a extremo");
        }
                
        if (!encryptionPort.verifyPin(plainCurrentPin, user.getPinHash())) {
            auditLogService.logFailure(userId, AuditLogService.PIN_CHANGED,
                    "USER", userId.toString(), "{\"reason\":\"INVALID_CURRENT_PIN\"}");
            pinAttemptService.checkAndRecordFailure(userId);
            throw new BusinessRuleException("INVALID_PIN", "El PIN actual es incorrecto");
        }
        
        String newPinHash = encryptionPort.hashPin(plainNewPin);
        User updatedUser = user.withPinHash(newPinHash);
        userRepository.save(updatedUser);
        
        pinAttemptService.resetAttempts(userId);

        auditLogService.logSuccess(userId, AuditLogService.PIN_CHANGED,
                "USER", userId.toString(), null);
    }
}
