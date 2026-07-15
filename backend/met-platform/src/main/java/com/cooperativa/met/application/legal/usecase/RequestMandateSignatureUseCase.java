package com.cooperativa.met.application.legal.usecase;

import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestMandateSignatureUseCase {

    private final UserRepositoryPort userRepository;
    private final OtpService otpService;

    public String execute(UUID userId) {
        log.info("Usuario {} solicita firma de mandato. Enviando OTP.", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("USR_01", "Usuario no encontrado"));

        // Se reusa el OtpService para enviar el OTP al correo
        otpService.generateAndSendOtp(user.getDocumentNumber(), user.getEmail());
        
        // Retornamos un ID de transacción para que el cliente lo mande en la confirmación.
        // Como el OtpService actual usa el documentNumber como llave en Redis, 
        // usaremos el documentNumber enmascarado como referencia por ahora.
        return "tx_" + UUID.randomUUID().toString(); 
    }
}
