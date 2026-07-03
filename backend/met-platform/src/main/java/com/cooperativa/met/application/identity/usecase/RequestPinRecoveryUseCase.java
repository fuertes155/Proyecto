package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.PinRecoveryRequest;
import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
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
        // Find user to get the email
        User user = userRepository.findByDocument(request.getDocumentType(), request.getDocumentNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con este documento"));

        // Generate and send OTP via email
        otpService.generateAndSendOtp(request.getDocumentNumber(), user.getEmail());
    }
}
