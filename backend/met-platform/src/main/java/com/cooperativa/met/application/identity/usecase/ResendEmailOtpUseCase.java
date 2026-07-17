package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.ResendEmailOtpRequest;
import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResendEmailOtpUseCase {

    private final UserRepositoryPort userRepository;
    private final OtpService otpService;

    public void execute(ResendEmailOtpRequest request) {
        var user = userRepository.findByDocument(request.documentType(), request.documentNumber())
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "Usuario no encontrado"));

        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new BusinessRuleException("ALREADY_VERIFIED",
                    "El correo de esta cuenta ya ha sido verificado anteriormente.");
        }

        otpService.generateAndSendEmailVerificationOtp(user.getDocumentNumber(), user.getEmail());
    }
}
