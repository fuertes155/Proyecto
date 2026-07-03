package com.cooperativa.met.application.identity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final String OTP_PREFIX = "pin-recovery-otp:";
    private static final Duration OTP_EXPIRY = Duration.ofMinutes(5);

    public String generateAndSendOtp(String documentNumber) {
        // Generate a 6-digit OTP
        int otpNum = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(otpNum);

        // Save in Redis
        String key = OTP_PREFIX + documentNumber;
        redisTemplate.opsForValue().set(key, otpCode, OTP_EXPIRY);

        // Simulate sending via SMS/Email
        log.info("=========================================================");
        log.info("SIMULATED OTP SENDING");
        log.info("To Document: {}", documentNumber);
        log.info("OTP Code: {}", otpCode);
        log.info("=========================================================");

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
