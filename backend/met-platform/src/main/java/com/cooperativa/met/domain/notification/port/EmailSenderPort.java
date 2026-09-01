package com.cooperativa.met.domain.notification.port;

/**
 * Puerto de salida para el envío de correos transaccionales (OTP, alertas de
 * seguridad, verificación de cuenta).
 *
 * <p>La implementación concreta se elige con la propiedad {@code met.mail.provider}:
 * <ul>
 *   <li>{@code smtp} (por defecto) — {@code SmtpEmailSender} sobre JavaMailSender.
 *       Sirve en local; NO en Render (bloquea el SMTP saliente).</li>
 *   <li>{@code brevo} — {@code BrevoEmailSender} sobre la API HTTP de Brevo.</li>
 * </ul>
 */
public interface EmailSenderPort {

    /**
     * Envía un correo de texto plano.
     *
     * @throws EmailDeliveryException si el proveedor rechaza o no acepta el envío.
     */
    void sendPlainText(String to, String subject, String body);

    /** El proveedor no pudo entregar el correo. */
    class EmailDeliveryException extends RuntimeException {
        public EmailDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }

        public EmailDeliveryException(String message) {
            super(message);
        }
    }
}
