package com.cooperativa.met.application.identity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private static final String OTP_PREFIX = "pin-recovery-otp:";
    private static final Duration OTP_EXPIRY = Duration.ofMinutes(5);

    public String generateAndSendOtp(String documentNumber, String email) {
        // Generate a 6-digit OTP
        int otpNum = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(otpNum);

        // Save in Redis
        String key = OTP_PREFIX + documentNumber;
        redisTemplate.opsForValue().set(key, otpCode, OTP_EXPIRY);

        // Send Email
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Tu código de recuperación de PIN");
            message.setText("Hola,\n\nHas solicitado recuperar tu PIN. Tu código de verificación es: " + otpCode + 
                            "\n\nEste código expirará en 5 minutos.\nSi no fuiste tú, ignora este mensaje.\n\nSaludos,\nEquipo de Finanzas");
            mailSender.send(message);
            log.info("OTP Email sent successfully to {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP Email to {}", email, e);
            log.warn("=================================================");
            log.warn("MOCK DEV MODE - Tu código OTP es: {}", otpCode);
            log.warn("=================================================");
        }

        return otpCode;
    }

    public boolean validateOtp(String documentNumber, String inputOtp) {
        String key = OTP_PREFIX + documentNumber;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp != null && storedOtp.equals(inputOtp)) {
            // OTP is valid, remove it so it can't be used again
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}
