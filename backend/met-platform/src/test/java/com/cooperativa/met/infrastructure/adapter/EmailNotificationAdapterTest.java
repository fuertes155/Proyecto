package com.cooperativa.met.infrastructure.adapter;

import com.cooperativa.met.domain.notification.port.EmailSenderPort;
import com.cooperativa.met.domain.notification.port.EmailSenderPort.EmailDeliveryException;
import com.cooperativa.met.infrastructure.security.PinAttemptService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Correos de seguridad que recibe el socio (ingreso desde una IP nueva, cuenta bloqueada). */
class EmailNotificationAdapterTest {

    private final EmailSenderPort emailSender = mock(EmailSenderPort.class);
    private final EmailNotificationAdapter adapter = new EmailNotificationAdapter(emailSender);

    private String cuerpoEnviadoA(String correo, String asunto) {
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendPlainText(eq(correo), eq(asunto), cuerpo.capture());
        return cuerpo.getValue();
    }

    @Test
    void avisaDeUnIngresoDesdeUnaIpNuevaIncluyendoLaIp() {
        adapter.sendNewLoginFromNewIpEmail("socio@correo.com", "181.50.12.34");

        String cuerpo = cuerpoEnviadoA("socio@correo.com", "Alerta de Seguridad: Nuevo inicio de sesión");
        assertTrue(cuerpo.contains("IP: 181.50.12.34"), cuerpo);
        assertTrue(cuerpo.contains("cambia tu PIN"), cuerpo);
    }

    @Test
    void elCorreoDeBloqueoDiceLosIntentosYElTiempoDeLaReglaReal() {
        adapter.sendAccountLockedEmail("socio@correo.com");

        String cuerpo = cuerpoEnviadoA("socio@correo.com", "Cuenta Bloqueada");
        assertTrue(cuerpo.contains(PinAttemptService.MAX_ATTEMPTS + " intentos"), cuerpo);
        assertTrue(cuerpo.contains(PinAttemptService.BLOCK_DURATION.toMinutes() + " minutos"), cuerpo);
    }

    @Test
    void unFalloDelCorreoNoInterrumpeElIngreso() {
        doThrow(new EmailDeliveryException("Brevo respondió 500")).when(emailSender).sendPlainText(any(), any(), any());

        assertDoesNotThrow(() -> adapter.sendNewLoginFromNewIpEmail("socio@correo.com", "181.50.12.34"));
        assertDoesNotThrow(() -> adapter.sendAccountLockedEmail("socio@correo.com"));
    }
}
