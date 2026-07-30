package com.cooperativa.met.application.identity.service;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${met.mail.from-address}")
    private String fromAddress;

    // Solo development/local (ver application-dev.yml). En producción debe quedar en false
    // para que un fallo de envío se reporte como error real en vez de exponer el OTP en logs.
    @Value("${met.mail.log-otp-on-failure:false}")
    private boolean logOtpOnFailure;

    private static final String OTP_PREFIX = "pin-recovery-otp:";
    private static final String OTP_ATTEMPTS_PREFIX = "otp-attempts:";
    private static final String OTP_LOCKOUT_PREFIX = "otp-lockout:";
    private static final Duration OTP_EXPIRY = Duration.ofMinutes(5);
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS = 3;

    public String generateAndSendOtp(String documentNumber, String email) {
        checkLockout(documentNumber);

        // Generate a 6-digit OTP
        int otpNum = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(otpNum);

        // Save in Redis
        String key = OTP_PREFIX + documentNumber;
        redisTemplate.opsForValue().set(key, otpCode, OTP_EXPIRY);
        
        // Reset attempts
        redisTemplate.delete(OTP_ATTEMPTS_PREFIX + documentNumber);

        // Send Email
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(email);
            message.setSubject("Tu código de recuperación de PIN");
            message.setText("Hola,\n\nHas solicitado recuperar tu PIN. Tu código de verificación es: " + otpCode +
                            "\n\nEste código expirará en 5 minutos.\nSi no fuiste tú, ignora este mensaje.\n\nSaludos,\nEquipo de Finanzas");
            mailSender.send(message);
            log.info("OTP Email sent successfully to {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP Email to {}", email, e);
            if (logOtpOnFailure) {
                log.warn("=================================================");
                log.warn("MOCK DEV MODE - Tu código OTP es: {}", otpCode);
                log.warn("=================================================");
            } else {
                throw new BusinessRuleException("EMAIL_DELIVERY_FAILED",
                    "No pudimos enviar el correo con tu código. Intenta nuevamente en unos minutos.");
            }
        }

        return otpCode;
    }

    public boolean validateOtp(String documentNumber, String inputOtp) {
        checkLockout(documentNumber);
        
        String key = OTP_PREFIX + documentNumber;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            return false;
        }

        if (storedOtp.equals(inputOtp)) {
            // OTP is valid, remove it so it can't be used again
            redisTemplate.delete(key);
            redisTemplate.delete(OTP_ATTEMPTS_PREFIX + documentNumber);
            return true;
        }
        
        handleFailedAttempt(documentNumber, key);
        return false;
    }

    public String generateAndSendEmailVerificationOtp(String documentNumber, String email) {
        checkLockout(documentNumber);
        
        int otpNum = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(otpNum);
        String key = "email-verification-otp:" + documentNumber;
        redisTemplate.opsForValue().set(key, otpCode, OTP_EXPIRY);
        redisTemplate.delete(OTP_ATTEMPTS_PREFIX + documentNumber);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(email);
            message.setSubject("Verificación de Correo Electrónico");
            message.setText("Hola,\n\nTu código de verificación de correo es: " + otpCode +
                            "\n\nEste código expirará en 5 minutos.\n\nSaludos,\nEquipo MET");
            mailSender.send(message);
            log.info("Email Verification OTP sent successfully to {}", email);
        } catch (Exception e) {
            log.error("Failed to send Email Verification OTP to {}", email, e);
            if (logOtpOnFailure) {
                log.warn("MOCK DEV MODE - Tu código OTP de verificación es: {}", otpCode);
            } else {
                throw new BusinessRuleException("EMAIL_DELIVERY_FAILED",
                    "No pudimos enviar el correo de verificación. Intenta nuevamente en unos minutos.");
            }
        }
        return otpCode;
    }

    public boolean validateEmailVerificationOtp(String documentNumber, String inputOtp) {
        checkLockout(documentNumber);
        
        String key = "email-verification-otp:" + documentNumber;
        String storedOtp = redisTemplate.opsForValue().get(key);
        
        if (storedOtp == null) return false;
        
        if (storedOtp.equals(inputOtp)) {
            redisTemplate.delete(key);
            redisTemplate.delete(OTP_ATTEMPTS_PREFIX + documentNumber);
            return true;
        }
        
        handleFailedAttempt(documentNumber, key);
        return false;
    }
    
    private void checkLockout(String documentNumber) {
        String lockoutKey = OTP_LOCKOUT_PREFIX + documentNumber;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockoutKey))) {
            throw new com.cooperativa.met.domain.common.exception.BusinessRuleException(
                "OTP_LOCKED", "Has superado el límite de intentos. Por seguridad, debes esperar 15 minutos para volver a intentar.");
        }
    }
    
    private void handleFailedAttempt(String documentNumber, String otpKey) {
        String attemptsKey = OTP_ATTEMPTS_PREFIX + documentNumber;
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        
        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            // Lock out user for OTPs
            redisTemplate.opsForValue().set(OTP_LOCKOUT_PREFIX + documentNumber, "LOCKED", LOCKOUT_DURATION);
            // Delete the active OTP to invalidate it completely
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptsKey);
            throw new com.cooperativa.met.domain.common.exception.BusinessRuleException(
                "TOO_MANY_OTP_ATTEMPTS", "Has ingresado el código incorrectamente " + MAX_ATTEMPTS + " veces. El código ha sido invalidado por seguridad.");
        }
    }
}
