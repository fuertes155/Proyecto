package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.PinRecoveryRequest;
import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestPinRecoveryUseCase {

    private final UserRepositoryPort userRepository;
    private final OtpService otpService;

    @Transactional(readOnly = true)
    public void execute(PinRecoveryRequest request) {
        boolean exists = userRepository.existsByDocument(request.getDocumentType(), request.getDocumentNumber());
        if (!exists) {
            // For security, don't throw an error indicating the user doesn't exist,
            // just silently return, or throw a generic error depending on business rules.
            // But since the user wants error messages, we can throw not found.
            throw new ResourceNotFoundException("Usuario no encontrado con este documento");
        }

        // Generate and send OTP
        otpService.generateAndSendOtp(request.getDocumentNumber());
    }
}
