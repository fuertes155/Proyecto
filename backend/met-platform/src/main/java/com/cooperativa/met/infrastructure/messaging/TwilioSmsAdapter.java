package com.cooperativa.met.infrastructure.messaging;

import com.cooperativa.met.domain.lending.port.MessagingPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TwilioSmsAdapter implements MessagingPort {

    @Override
    public void sendSms(String phoneNumber, String message) {
        log.info("==============================================");
        log.info("[TWILIO MOCK SMS] Enviando SMS a {}", phoneNumber);
        log.info("Contenido: {}", message);
        log.info("==============================================");
    }

    @Override
    public void sendWhatsApp(String phoneNumber, String message) {
        log.info("==============================================");
        log.info("[TWILIO MOCK WHATSAPP] Enviando WA a {}", phoneNumber);
        log.info("Contenido: {}", message);
        log.info("==============================================");
    }
}
