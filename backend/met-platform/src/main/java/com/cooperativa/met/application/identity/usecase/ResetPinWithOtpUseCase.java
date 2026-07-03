package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.PinRecoveryResetRequest;
import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResetPinWithOtpUseCase {

    private final UserRepositoryPort userRepository;
    private final OtpService otpService;
    private final EncryptionPort encryptionPort;

    @Transactional
    public void execute(PinRecoveryResetRequest request) {
        User user = userRepository.findByDocument(request.getDocumentType(), request.getDocumentNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        boolean isOtpValid = otpService.validateOtp(request.getDocumentNumber(), request.getOtpCode());
        if (!isOtpValid) {
            throw new BusinessRuleException("INVALID_OTP", "El código de verificación es incorrecto o ha expirado");
        }

        String newPinHash = encryptionPort.hashPin(request.getNewPin());
        User updatedUser = user.withPinHash(newPinHash);
        userRepository.save(updatedUser);
    }
}
