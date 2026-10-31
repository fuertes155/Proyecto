package com.cooperativa.met.infrastructure.email;

import com.cooperativa.met.domain.notification.port.EmailSenderPort.EmailDeliveryException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Envío por SMTP (el proveedor de correo en local). */
class SmtpEmailSenderTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SmtpEmailSender sender = new SmtpEmailSender(mailSender, "no-responder@met.coop");

    @Test
    void armaElMensajeConRemitenteDestinatarioAsuntoYTexto() {
        sender.sendPlainText("socio@correo.com", "Tu código de verificación", "Tu código es 482913");

        ArgumentCaptor<SimpleMailMessage> mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mensaje.capture());
        assertEquals("no-responder@met.coop", mensaje.getValue().getFrom());
        assertArrayEquals(new String[]{"socio@correo.com"}, mensaje.getValue().getTo());
        assertEquals("Tu código de verificación", mensaje.getValue().getSubject());
        assertEquals("Tu código es 482913", mensaje.getValue().getText());
    }

    @Test
    void avisaCuandoElServidorSmtpNoPuedeEnviar() {
        doThrow(new MailSendException("Connection refused")).when(mailSender).send(any(SimpleMailMessage.class));

        EmailDeliveryException error = assertThrows(EmailDeliveryException.class,
                () -> sender.sendPlainText("socio@correo.com", "Asunto", "Texto"));

        assertTrue(error.getMessage().contains("socio@correo.com"), error.getMessage());
        assertInstanceOf(MailSendException.class, error.getCause());
    }
}
