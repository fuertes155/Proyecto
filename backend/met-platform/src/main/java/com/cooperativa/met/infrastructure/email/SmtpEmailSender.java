package com.cooperativa.met.infrastructure.email;

import com.cooperativa.met.domain.notification.port.EmailSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Envío por SMTP (JavaMailSender). Adaptador por defecto — funciona en local.
 *
 * <p>En Render NO sirve: la plataforma bloquea los puertos SMTP salientes
 * (25/465/587) en todos los planes. Allí se usa {@link BrevoEmailSender}
 * ({@code MAIL_PROVIDER=brevo}).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "met.mail.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailSender implements EmailSenderPort {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailSender(JavaMailSender mailSender,
                           @Value("${met.mail.from-address}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        log.info("Email provider: SMTP (from {})", fromAddress);
    }

    @Override
    public void sendPlainText(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("SMTP email sent to {}", to);
        } catch (MailException e) {
            throw new EmailDeliveryException("SMTP no pudo enviar el correo a " + to, e);
        }
    }
}
