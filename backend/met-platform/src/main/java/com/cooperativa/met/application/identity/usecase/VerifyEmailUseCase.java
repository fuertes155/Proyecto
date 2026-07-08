package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.VerifyEmailRequest;
import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerifyEmailUseCase {
    private final UserRepositoryPort userRepository;
    private final OtpService otpService;

    @Transactional
    public void execute(VerifyEmailRequest request) {
        User user = userRepository.findByDocument(request.documentType(), request.documentNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new BusinessRuleException("ALREADY_VERIFIED", "El correo ya está verificado o la cuenta está en otro estado.");
        }

        boolean isValid = otpService.validateEmailVerificationOtp(request.documentNumber(), request.otpCode());
        if (!isValid) {
            throw new BusinessRuleException("INVALID_OTP", "El código es inválido o ha expirado.");
        }

        User updatedUser = user.withStatus(UserStatus.ACTIVE);
        userRepository.save(updatedUser);
    }
}
