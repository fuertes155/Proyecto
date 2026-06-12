package com.cooperativa.fintech.application.identity.usecase;

import com.cooperativa.fintech.application.identity.dto.AuthResponse;
import com.cooperativa.fintech.application.identity.dto.LoginRequest;
import com.cooperativa.fintech.domain.common.exception.BusinessRuleException;
import com.cooperativa.fintech.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.fintech.domain.identity.model.User;
import com.cooperativa.fintech.domain.identity.model.UserStatus;
import com.cooperativa.fintech.domain.identity.port.EncryptionPort;
import com.cooperativa.fintech.domain.identity.port.TokenPort;
import com.cooperativa.fintech.domain.identity.port.UserRepositoryPort;
import com.cooperativa.fintech.infrastructure.config.FintechSecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserRepositoryPort userRepository;
    private final EncryptionPort encryptionPort;
    private final TokenPort tokenPort;
    private final FintechSecurityProperties securityProperties;

    @Transactional(readOnly = true)
    public AuthResponse execute(LoginRequest request) {
        User user = userRepository.findByDocument(request.documentType(), request.documentNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Credenciales inválidas"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("USER_NOT_ACTIVE", "La cuenta no está activa");
        }

        boolean authenticated = authenticate(request, user);
        if (!authenticated) {
            throw new BusinessRuleException("INVALID_CREDENTIALS", "Credenciales inválidas");
        }

        String accessToken = tokenPort.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = tokenPort.generateRefreshToken(user.getId());

        return AuthResponse.of(
                user.getId(),
                accessToken,
                refreshToken,
                securityProperties.getJwt().getExpirationMs()
        );
    }

    private boolean authenticate(LoginRequest request, User user) {
        if (StringUtils.hasText(request.biometricPayload())) {
            if (user.getBiometricHash() == null) {
                return false;
            }
            String payloadHash = encryptionPort.hashBiometric(request.biometricPayload());
            return payloadHash.equals(user.getBiometricHash());
        }
        if (StringUtils.hasText(request.pin())) {
            return encryptionPort.verifyPin(request.pin(), user.getPinHash());
        }
        return false;
    }
}
