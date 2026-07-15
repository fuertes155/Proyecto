package com.cooperativa.met.domain.investment.service;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LegalSignatureService {

    // Simple In-Memory Cache for OTP (In production, use Redis with TTL)
    private final Map<String, String> otpCache = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a 6-digit OTP and simulates sending it via Twilio SMS.
     */
    public String generateAndSendOtp(String phoneNumber) {
        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        String transactionId = UUID.randomUUID().toString();
        
        otpCache.put(transactionId, otp);
        
        // Simulating Twilio API Call
        log.info("[TWILIO MOCK] Enviaremos el SMS al {}: 'Tu código de firma electrónica para MET Cooperativa es: {}'", phoneNumber, otp);
        
        return transactionId;
    }

    /**
     * Validates the OTP against the transaction ID.
     */
    public boolean validateOtp(String transactionId, String otpCode) {
        String cachedOtp = otpCache.get(transactionId);
        if (cachedOtp != null && cachedOtp.equals(otpCode)) {
            otpCache.remove(transactionId); // Burn the OTP after use
            return true;
        }
        return false;
    }

    /**
     * Simulates rendering an HTML template to a PDF byte array using OpenHTMLToPDF.
     * In a real implementation, this would take the Thymeleaf Context and call PdfRendererBuilder.
     */
    public byte[] generateMandatePdf(String userName, String amount, String date) {
        log.info("Rendering PDF Contract using OpenHTMLToPDF for user {}", userName);
        
        String simulatedPdfContent = "--- CONTRATO DE MANDATO ---\n" +
                "Usuario: " + userName + "\n" +
                "Monto: " + amount + "\n" +
                "Fecha: " + date + "\n" +
                "Firma Electrónica Validada vía OTP.\n" +
                "---------------------------\n";
                
        return simulatedPdfContent.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Calculates the SHA-256 hash of a document (byte array).
     */
    public String calculateSha256(byte[] documentBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(documentBytes);
            return Base64.getEncoder().encodeToString(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessRuleException("SYS_ERR", "Error calculando el hash del documento");
        }
    }
}
