package com.cooperativa.met.infrastructure.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** SMS y WhatsApp por la API de Twilio (recordatorios de cobro), contra un servidor simulado. */
class TwilioSmsAdapterTest {

    private static final String MENSAJES = "https://api.twilio.com/2010-04-01/Accounts/AC123/Messages.json";

    private TwilioSmsAdapter adapter;
    private MockRestServiceServer twilio;

    @BeforeEach
    void setUp() {
        adapter = new TwilioSmsAdapter();
        ReflectionTestUtils.setField(adapter, "accountSid", "AC123");
        ReflectionTestUtils.setField(adapter, "authToken", "token-secreto");
        ReflectionTestUtils.setField(adapter, "fromPhoneNumber", "+15005550006");
        ReflectionTestUtils.setField(adapter, "fromWhatsappNumber", "+14155238886");
        twilio = MockRestServiceServer.bindTo((RestTemplate) ReflectionTestUtils.getField(adapter, "restTemplate")).build();
    }

    private static MultiValueMap<String, String> formulario(String para, String de, String texto) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", para);
        form.add("From", de);
        form.add("Body", texto);
        return form;
    }

    @Test
    void enviaElSmsConAutenticacionBasicaYElFormularioDeTwilio() {
        String credenciales = Base64.getEncoder().encodeToString("AC123:token-secreto".getBytes(StandardCharsets.UTF_8));
        twilio.expect(requestTo(MENSAJES))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic " + credenciales))
                .andExpect(header("Content-Type", startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                .andExpect(content().formData(formulario("+573001234567", "+15005550006", "Tu cuota de $50.000 vence mañana")))
                .andRespond(withSuccess("{\"sid\":\"SM1\"}", MediaType.APPLICATION_JSON));

        adapter.sendSms("+573001234567", "Tu cuota de $50.000 vence mañana");

        twilio.verify();
    }

    @Test
    void enviaWhatsAppAnteponiendoElPrefijoQuePideTwilio() {
        twilio.expect(requestTo(MENSAJES))
                .andExpect(content().formData(formulario("whatsapp:+573001234567", "whatsapp:+14155238886", "Hola")))
                .andRespond(withSuccess("{\"sid\":\"SM2\"}", MediaType.APPLICATION_JSON));

        adapter.sendWhatsApp("+573001234567", "Hola");

        twilio.verify();
    }

    @Test
    void conLaCuentaSimuladaSoloRegistraElMensajeSinLlamarATwilio() {
        ReflectionTestUtils.setField(adapter, "accountSid", "mock-sid");

        adapter.sendSms("+573001234567", "Hola");
        adapter.sendWhatsApp("+573001234567", "Hola");

        twilio.verify(); // sin expectativas: cualquier llamada a Twilio habría hecho fallar la prueba
    }

    @Test
    void unErrorDeTwilioNoDetieneElProcesoQueEnviaElMensaje() {
        twilio.expect(requestTo(MENSAJES))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"code\":20003}").contentType(MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> adapter.sendSms("+573001234567", "Recordatorio de pago"));

        twilio.verify();
    }
}
