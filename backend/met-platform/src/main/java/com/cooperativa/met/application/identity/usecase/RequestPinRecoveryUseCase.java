package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.PinRecoveryRequest;
import com.cooperativa.met.application.identity.service.OtpService;
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
        // Responde igual exista o no el documento, para no permitir enumerar usuarios
        // registrados a través de este endpoint público. Solo se envía el OTP si hay match.
        userRepository.findByDocument(request.getDocumentType(), request.getDocumentNumber())
                .ifPresent(user -> otpService.generateAndSendOtp(request.getDocumentNumber(), user.getEmail()));
    }
}
