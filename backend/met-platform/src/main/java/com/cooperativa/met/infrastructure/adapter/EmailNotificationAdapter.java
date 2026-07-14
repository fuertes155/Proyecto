package com.cooperativa.met.infrastructure.adapter;

import com.cooperativa.met.domain.identity.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Adaptador de infraestructura que implementa {@link NotificationPort}
 * usando Spring Mail (JavaMailSender) para el envío de emails de seguridad.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationAdapter implements NotificationPort {

    private final JavaMailSender mailSender;

    @Override
    public void sendAccountLockedEmail(String email) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@met.com");
            message.setTo(email);
            message.setSubject("Cuenta Bloqueada");
            message.setText(
                "Hola,\n\n" +
                "Tu cuenta ha sido bloqueada tras 3 intentos fallidos de inicio de sesión.\n\n" +
                "Por favor, contacta a soporte para desbloquearla.\n\n" +
                "Saludos,\nEquipo MET"
            );
            mailSender.send(message);
            log.info("Account locked email sent successfully");
        } catch (Exception e) {
            log.error("Failed to send account locked email: {}", e.getMessage());
        }
    }

    @Override
    public void sendNewLoginFromNewIpEmail(String email, String ip) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("security@met.com");
            message.setTo(email);
            message.setSubject("Alerta de Seguridad: Nuevo inicio de sesión");
            message.setText(
                "Hola,\n\n" +
                "Hemos detectado un inicio de sesión en tu cuenta desde una nueva ubicación.\n" +
                "Si no fuiste tú, por favor cambia tu PIN inmediatamente y contacta a soporte.\n\n" +
                "Saludos,\nEquipo de Seguridad MET"
            );
            mailSender.send(message);
            log.info("New IP login alert email sent successfully");
        } catch (Exception e) {
            log.error("Failed to send new IP login alert email: {}", e.getMessage());
        }
    }
}
