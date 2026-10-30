package com.cooperativa.met.infrastructure.config;

import com.cooperativa.met.TestRedisConfig;
import com.cooperativa.met.infrastructure.web.webhook.MockPaymentGatewayController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Perfiles de Render para el sandbox: {@code prod,demo,sandbox}. "sandbox" va de último y gana en la
 * pasarela de depósitos (Wompi real), mientras que de "demo" se conserva el monto del micro-depósito
 * en los logs, sin el cual no se pueden verificar cuentas bancarias ni probar retiros.
 * ("prod" se reemplaza por "test" para no depender de Vault ni de PostgreSQL.)
 */
@SpringBootTest(properties = "met.wompi.sync-banks-on-startup=false")
@ActiveProfiles({"test", "demo", "sandbox"})
@Import(TestRedisConfig.class)
class SandboxProfileCombinationTest {

    @Autowired
    private Environment environment;
    @Autowired
    private ApplicationContext context;

    @Test
    void losDepositosVanAlSandboxRealDeWompiMientrasLosRetirosSiguenSimulados() {
        assertEquals("wompi", environment.getProperty("met.payments.gateway"));
        assertEquals("mock", environment.getProperty("met.payments.payout-gateway"));
        assertEquals("https://sandbox.wompi.co/v1", environment.getProperty("met.wompi.api-url"));
        assertTrue(context.getBeansOfType(MockPaymentGatewayController.class).isEmpty(),
                "la pasarela simulada no debe existir: solo el webhook firmado de Wompi acredita saldo");
    }

    @Test
    void dejaElMontoDelMicroDepositoEnLosRegistrosParaVerificarCuentasBancarias() {
        assertEquals(Boolean.TRUE, environment.getProperty("met.dev.log-verification-amount", Boolean.class));
    }
}
