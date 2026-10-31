package com.cooperativa.met.infrastructure.email;

import com.cooperativa.met.domain.notification.port.EmailSenderPort.EmailDeliveryException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Envío por la API HTTP de Brevo, contra un servidor HTTP local que hace de Brevo. */
class BrevoEmailSenderTest {

    private final ObjectMapper json = new ObjectMapper();
    private final AtomicReference<String> recibido = new AtomicReference<>();
    private final AtomicReference<Headers> cabeceras = new AtomicReference<>();
    private volatile int estado = 201;
    private volatile String respuesta = "{\"messageId\":\"<abc@smtp-relay.brevo.com>\"}";
    private HttpServer brevo;

    @BeforeEach
    void setUp() throws IOException {
        brevo = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        brevo.createContext("/v3/smtp/email", intercambio -> {
            cabeceras.set(intercambio.getRequestHeaders());
            recibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] cuerpo = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(estado, cuerpo.length);
            intercambio.getResponseBody().write(cuerpo);
            intercambio.close();
        });
        brevo.start();
    }

    @AfterEach
    void tearDown() {
        brevo.stop(0);
    }

    private BrevoEmailSender sender() {
        return new BrevoEmailSender("  xkeysib-clave-de-prueba  ", "no-responder@met.coop", "MET Cooperativa",
                "http://127.0.0.1:" + brevo.getAddress().getPort() + "/v3/smtp/email");
    }

    @Test
    void enviaElCorreoConLaLlaveYElRemitenteConfigurados() throws Exception {
        sender().sendPlainText("socio@correo.com", "Tu código de verificación", "Tu código es 482913");

        assertEquals("xkeysib-clave-de-prueba", cabeceras.get().getFirst("api-key"), "la llave va sin espacios");
        assertEquals("application/json", cabeceras.get().getFirst("content-type"));
        JsonNode correo = json.readTree(recibido.get());
        assertEquals("MET Cooperativa", correo.at("/sender/name").asText());
        assertEquals("no-responder@met.coop", correo.at("/sender/email").asText());
        assertEquals("socio@correo.com", correo.at("/to/0/email").asText());
        assertEquals("Tu código de verificación", correo.at("/subject").asText());
        assertEquals("Tu código es 482913", correo.at("/textContent").asText());
    }

    @Test
    void escapaComillasSaltosDeLineaYCaracteresDeControlSinPerderLasTildes() throws Exception {
        // El JSON se arma a mano: si algún carácter no se escapa bien, Brevo recibe un JSON roto
        // y el socio se queda sin su código.
        String asunto = "Código \"OTP\" para José Muñoz \\ MET";
        String texto = "Hola,\n\tTu código es 482913.\r\nNo lo compartas.\u0001 ¡Gracias! 👋";

        sender().sendPlainText("socio@correo.com", asunto, texto);

        JsonNode correo = json.readTree(recibido.get());
        assertEquals(asunto, correo.at("/subject").asText());
        assertEquals(texto, correo.at("/textContent").asText());
    }

    @Test
    void avisaConElCodigoYElMotivoCuandoBrevoRechazaElEnvio() {
        estado = 401;
        respuesta = "{\"code\":\"unauthorized\",\"message\":\"Key not found\"}";

        EmailDeliveryException error = assertThrows(EmailDeliveryException.class,
                () -> sender().sendPlainText("socio@correo.com", "Asunto", "Texto"));

        assertTrue(error.getMessage().contains("401"), error.getMessage());
        assertTrue(error.getMessage().contains("Key not found"), error.getMessage());
    }

    @Test
    void avisaCuandoNoPuedeConectarseConBrevo() {
        BrevoEmailSender sender = sender();
        brevo.stop(0);

        EmailDeliveryException error = assertThrows(EmailDeliveryException.class,
                () -> sender.sendPlainText("socio@correo.com", "Asunto", "Texto"));

        assertTrue(error.getMessage().contains("socio@correo.com"), error.getMessage());
        assertNotNull(error.getCause());
    }

    @Test
    void noArrancaSinLaLlaveDeApi() {
        assertThrows(IllegalStateException.class,
                () -> new BrevoEmailSender("   ", "no-responder@met.coop", "MET", "http://127.0.0.1:1/v3/smtp/email"));
        assertThrows(IllegalStateException.class,
                () -> new BrevoEmailSender(null, "no-responder@met.coop", "MET", "http://127.0.0.1:1/v3/smtp/email"));
    }
}
