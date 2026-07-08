package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestTransferOtpUseCase {

    private final UserRepositoryPort userRepository;
    private final OtpService otpService;

    public void execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        otpService.generateAndSendOtp(user.getDocumentNumber(), user.getEmail());
    }
}
