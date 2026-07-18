package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.UpdatePinRequest;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.audit.AuditLogService;
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

    @Transactional
    public void execute(UUID userId, UpdatePinRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
                
        if (!encryptionPort.verifyPin(request.getCurrentPin(), user.getPinHash())) {
            auditLogService.logFailure(userId, AuditLogService.PIN_CHANGED,
                    "USER", userId.toString(), "{\"reason\":\"INVALID_CURRENT_PIN\"}");
            throw new BusinessRuleException("INVALID_PIN", "El PIN actual es incorrecto");
        }
        
        String newPinHash = encryptionPort.hashPin(request.getNewPin());
        User updatedUser = user.withPinHash(newPinHash);
        userRepository.save(updatedUser);

        auditLogService.logSuccess(userId, AuditLogService.PIN_CHANGED,
                "USER", userId.toString(), null);
    }
}
