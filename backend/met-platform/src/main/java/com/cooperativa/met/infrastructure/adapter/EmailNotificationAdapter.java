package com.cooperativa.met.infrastructure.adapter;

import com.cooperativa.met.domain.identity.port.NotificationPort;
import com.cooperativa.met.domain.notification.port.EmailSenderPort;
import com.cooperativa.met.infrastructure.security.PinAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adaptador de infraestructura que implementa {@link NotificationPort}
 * delegando el envío en {@link EmailSenderPort} (SMTP en local, Brevo en Render).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationAdapter implements NotificationPort {

    private final EmailSenderPort emailSender;

    @Override
    public void sendAccountLockedEmail(String email) {
        try {
            emailSender.sendPlainText(
                email,
                "Cuenta Bloqueada",
                "Hola,\n\n" +
                "Bloqueamos el ingreso a tu cuenta por " + PinAttemptService.BLOCK_DURATION.toMinutes() +
                " minutos tras " + PinAttemptService.MAX_ATTEMPTS + " intentos fallidos con tu PIN.\n\n" +
                "Pasado ese tiempo podrás volver a intentarlo. Si no fuiste tú, cambia tu PIN y contacta a soporte.\n\n" +
                "Saludos,\nEquipo MET"
            );
            log.info("Correo de cuenta bloqueada enviado");
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de cuenta bloqueada: {}", e.getMessage());
        }
    }

    @Override
    public void sendNewLoginFromNewIpEmail(String email, String ip) {
        try {
            emailSender.sendPlainText(
                email,
                "Alerta de Seguridad: Nuevo inicio de sesión",
                "Hola,\n\n" +
                "Hemos detectado un inicio de sesión en tu cuenta desde una nueva ubicación (IP: " + ip + ").\n" +
                "Si no fuiste tú, por favor cambia tu PIN inmediatamente y contacta a soporte.\n\n" +
                "Saludos,\nEquipo de Seguridad MET"
            );
            log.info("Correo de alerta de ingreso desde una IP nueva enviado");
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de alerta de ingreso desde una IP nueva: {}", e.getMessage());
        }
    }
}
