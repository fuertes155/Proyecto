package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.AuthResponse;
import com.cooperativa.met.application.identity.dto.LoginRequest;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.RefreshToken;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.NotificationPort;
import com.cooperativa.met.domain.identity.port.TokenPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.identity.port.RefreshTokenRepositoryPort;
import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import com.cooperativa.met.application.security.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserRepositoryPort userRepository;
    private final EncryptionPort encryptionPort;
    private final TokenPort tokenPort;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final MetSecurityProperties securityProperties;
    private final NotificationPort notificationPort;
    private final OtpService otpService;
    private final FraudDetectionService fraudDetectionService;

    @Transactional
    public AuthResponse execute(LoginRequest request, String ip) {
        log.debug("Login attempt for documentType={}", request.documentType());

        User user = userRepository.findByDocument(request.documentType(), request.documentNumber())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found for documentType={}", request.documentType());
                    return new BusinessRuleException("INVALID_CREDENTIALS", "Credenciales inválidas");
                });

        // 🔥 Fraude: Regla 1 (Bloqueo de VPN/Tor)
        fraudDetectionService.checkIpReputation(ip);

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new BusinessRuleException("ACCOUNT_LOCKED", "Tu cuenta está bloqueada temporalmente");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Login failed - user not active, status={}, userId={}", user.getStatus(), user.getId());
            throw new BusinessRuleException("USER_NOT_ACTIVE", "La cuenta no está activa");
        }

        boolean authenticated = authenticate(request, user);
        log.debug("Authentication result={} for userId={}", authenticated, user.getId());

        if (!authenticated) {
            int newAttempts = user.getFailedLoginAttempts() + 1;
            User updatedUser = user.withFailedLoginAttempts(newAttempts);
            if (newAttempts >= 3) {
                updatedUser = updatedUser.withStatus(UserStatus.BLOCKED);
                userRepository.save(updatedUser);
                notificationPort.sendAccountLockedEmail(updatedUser.getEmail());
                throw new BusinessRuleException("ACCOUNT_LOCKED", "Tu cuenta ha sido bloqueada temporalmente por demasiados intentos fallidos");
            }
            userRepository.save(updatedUser);
            throw new BusinessRuleException("INVALID_CREDENTIALS", "Credenciales inválidas. Intento " + newAttempts + " de 3");
        }

        // 🛡️ Fix Device Binding: Verificar si es un dispositivo nuevo
        if (!user.isDeviceRecognized(request.deviceId())) {
            if (request.otpCode() == null || request.otpCode().isBlank()) {
                otpService.generateAndSendOtp(user.getDocumentNumber(), user.getEmail());
                throw new BusinessRuleException("DEVICE_NOT_RECOGNIZED", "Nuevo dispositivo detectado. Se ha enviado un código de seguridad a tu correo/SMS.");
            } else {
                boolean isOtpValid = otpService.validateOtp(user.getDocumentNumber(), request.otpCode());
                if (!isOtpValid) {
                    throw new BusinessRuleException("INVALID_OTP", "El código de seguridad es incorrecto o ha expirado.");
                }
                // Si el OTP es válido, vinculamos el nuevo dispositivo
                user = user.withLastKnownDeviceId(request.deviceId());
            }
        } else if (user.getLastKnownDeviceId() == null || user.getLastKnownDeviceId().isBlank()) {
            // Primer inicio de sesión, vinculamos automáticamente
            user = user.withLastKnownDeviceId(request.deviceId());
        }

        if (user.getFailedLoginAttempts() > 0 || (user.getLastKnownIp() == null || !user.getLastKnownIp().equals(ip))) {
            if (user.getLastKnownIp() != null && !user.getLastKnownIp().equals(ip)) {
                notificationPort.sendNewLoginFromNewIpEmail(user.getEmail(), ip);
            }
            userRepository.save(user.withFailedLoginAttempts(0).withLastKnownIp(ip));
        }

        // 🔥 Fraude: Registrar ubicación de login exitoso para el motor de "Viaje Imposible"
        fraudDetectionService.recordLoginLocation(user.getId(), ip);

        String accessToken = tokenPort.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = tokenPort.generateRefreshToken(user.getId());

        // 🔒 Fix 7: Una sola sesión activa por usuario. Revocar cualquier sesión previa en otro dispositivo.
        refreshTokenRepository.revokeAllByUserId(user.getId());

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
            try {
                String plainPin = encryptionPort.decryptRsa(request.pin());
                return encryptionPort.verifyPin(plainPin, user.getPinHash());
            } catch (Exception e) {
                log.warn("E2EE Decryption failed during login for userId={}", user.getId());
                return false;
            }
        }
        return false;
    }
}
