package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.AuthResponse;
import com.cooperativa.met.application.identity.dto.LoginRequest;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.RefreshTokenRepositoryPort;
import com.cooperativa.met.domain.identity.port.TokenPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.identity.model.RefreshToken;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserRepositoryPort userRepository;
    private final EncryptionPort encryptionPort;
    private final TokenPort tokenPort;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final MetSecurityProperties securityProperties;
    private final JavaMailSender mailSender;

    @Transactional
    public AuthResponse execute(LoginRequest request, String ip) {
        System.out.println("Login attempt for doc: '" + request.documentNumber() + "' of type: '" + request.documentType() + "'");
        User user = userRepository.findByDocument(request.documentType(), request.documentNumber())
                .orElseThrow(() -> {
                    System.out.println("User not found: '" + request.documentType() + "' '" + request.documentNumber() + "'");
                    return new BusinessRuleException("INVALID_CREDENTIALS", "Credenciales inválidas");
                });

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new BusinessRuleException("ACCOUNT_LOCKED", "Tu cuenta está bloqueada temporalmente");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            System.out.println("User not active: " + user.getStatus());
            throw new BusinessRuleException("USER_NOT_ACTIVE", "La cuenta no está activa");
        }

        boolean authenticated = authenticate(request, user);
        System.out.println("Authenticated: " + authenticated + " for user " + user.getDocumentNumber());
        if (!authenticated) {
            int newAttempts = user.getFailedLoginAttempts() + 1;
            User updatedUser = user.withFailedLoginAttempts(newAttempts);
            if (newAttempts >= 3) {
                updatedUser = updatedUser.withStatus(UserStatus.BLOCKED);
                userRepository.save(updatedUser);
                sendAccountLockedEmail(updatedUser.getEmail());
                throw new BusinessRuleException("ACCOUNT_LOCKED", "Tu cuenta ha sido bloqueada temporalmente por demasiados intentos fallidos");
            }
            userRepository.save(updatedUser);
            throw new BusinessRuleException("INVALID_CREDENTIALS", "Credenciales inválidas. Intento " + newAttempts + " de 3");
        }

        if (user.getFailedLoginAttempts() > 0 || (user.getLastKnownIp() == null || !user.getLastKnownIp().equals(ip))) {
            if (user.getLastKnownIp() != null && !user.getLastKnownIp().equals(ip)) {
                sendFraudAlertEmail(user.getEmail(), ip);
            }
            userRepository.save(user.withFailedLoginAttempts(0).withLastKnownIp(ip));
        }

        String accessToken = tokenPort.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = tokenPort.generateRefreshToken(user.getId());

        // Persistir refresh token para permitir revocación/rotación
        var refreshClaims = tokenPort.validateRefreshTokenClaims(refreshToken);
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(securityProperties.getJwt().getRefreshExpirationMs());
        RefreshToken tokenEntity = new RefreshToken(
                refreshClaims.jti(),
                refreshClaims.userId(),
                now,
                expiresAt,
                false
        );
        refreshTokenRepository.save(tokenEntity);

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

    private void sendAccountLockedEmail(String email) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@met.com");
            message.setTo(email);
            message.setSubject("Cuenta Bloqueada");
            message.setText("Hola,\n\nTu cuenta ha sido bloqueada tras 3 intentos fallidos de inicio de sesión.\n\nPor favor, contacta a soporte para desbloquearla.\n\nSaludos,\nEquipo MET");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send account locked email to " + email + ": " + e.getMessage());
        }
    }

    private void sendFraudAlertEmail(String email, String ip) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("security@met.com");
            message.setTo(email);
            message.setSubject("Alerta de Seguridad: Nuevo inicio de sesión");
            message.setText("Hola,\n\nHemos detectado un inicio de sesión en tu cuenta desde una nueva ubicación (IP: " + ip + ").\n" +
                    "Si no fuiste tú, por favor cambia tu contraseña inmediatamente y contacta a soporte.\n\nSaludos,\nEquipo de Seguridad MET");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send fraud alert email to " + email + ": " + e.getMessage());
        }
    }
}
