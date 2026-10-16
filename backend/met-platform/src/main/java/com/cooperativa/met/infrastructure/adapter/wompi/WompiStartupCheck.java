package com.cooperativa.met.infrastructure.adapter.wompi;

import com.cooperativa.met.application.bank.usecase.SyncBankCatalogUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Al arrancar con la pasarela real de Wompi:
 * <ol>
 *   <li>Avisa si las llaves no corresponden al ambiente de {@code met.wompi.api-url}
 *       (sandbox exige {@code pub_test_}, {@code prv_test_}, {@code test_integrity_},
 *       {@code test_events_}; producción los {@code *_prod_*}). Una llave del ambiente
 *       equivocado hace que Wompi responda 401/422 sin más detalle.</li>
 *   <li>Con {@code met.wompi.sync-banks-on-startup=true} (perfil sandbox) trae el catálogo
 *       PSE de Wompi, para que el selector de bancos de la app tenga los bancos de prueba.</li>
 * </ol>
 * Nunca impide el arranque: solo deja advertencias en el log.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "met.payments.gateway", havingValue = "wompi", matchIfMissing = true)
@RequiredArgsConstructor
public class WompiStartupCheck implements ApplicationRunner {

    private final SyncBankCatalogUseCase syncBankCatalogUseCase;

    @Value("${met.wompi.api-url}")
    private String apiUrl;

    @Value("${met.wompi.public-key}")
    private String publicKey;

    @Value("${met.wompi.private-key}")
    private String privateKey;

    @Value("${met.wompi.integrity-secret}")
    private String integritySecret;

    @Value("${met.wompi.webhook-secret}")
    private String eventsSecret;

    @Value("${met.wompi.redirect-url:}")
    private String redirectUrl;

    @Value("${met.wompi.sync-banks-on-startup:false}")
    private boolean syncBanksOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        boolean sandbox = apiUrl != null && apiUrl.toLowerCase(Locale.ROOT).contains("sandbox");
        List<String> problems = credentialProblems(sandbox);
        if (problems.isEmpty()) {
            log.info("Wompi configurado contra {} ({}).", apiUrl, sandbox ? "SANDBOX" : "PRODUCCIÓN");
        } else {
            log.warn("Revisa la configuración de Wompi ({}): {}", sandbox ? "SANDBOX" : "PRODUCCIÓN",
                    String.join("; ", problems));
        }
        if (redirectUrl == null || redirectUrl.isBlank()) {
            log.warn("WOMPI_REDIRECT_URL no está definida: Wompi redirigirá a metapp://deposit/success, "
                    + "que el navegador del celular no sabe abrir. Usa https://<host>/api/v1/payments/wompi/return");
        }

        if (syncBanksOnStartup) {
            try {
                int updated = syncBankCatalogUseCase.executePseOnly();
                log.info("Catálogo PSE de Wompi sincronizado al arrancar: {} bancos.", updated);
            } catch (Exception e) {
                log.warn("No se pudo sincronizar el catálogo PSE de Wompi al arrancar: {}", e.getMessage());
            }
        }
    }

    List<String> credentialProblems(boolean sandbox) {
        String env = sandbox ? "test" : "prod";
        Map<String, String[]> expectations = new LinkedHashMap<>();
        expectations.put("WOMPI_PUBLIC_KEY", new String[]{publicKey, "pub_" + env + "_"});
        expectations.put("WOMPI_PRIVATE_KEY", new String[]{privateKey, "prv_" + env + "_"});
        expectations.put("WOMPI_INTEGRITY_SECRET", new String[]{integritySecret, env + "_integrity_"});
        expectations.put("WOMPI_WEBHOOK_SECRET (secreto de eventos)", new String[]{eventsSecret, env + "_events_"});

        List<String> problems = new ArrayList<>();
        expectations.forEach((name, valueAndPrefix) -> {
            String value = valueAndPrefix[0];
            String prefix = valueAndPrefix[1];
            if (value == null || value.isBlank()) {
                problems.add(name + " está vacía");
            } else if (value.toUpperCase(Locale.ROOT).contains("REPLACE") || value.contains("placeholder")) {
                problems.add(name + " tiene un valor de ejemplo");
            } else if (!value.startsWith(prefix)) {
                problems.add(name + " debería empezar por " + prefix);
            }
        });
        return problems;
    }
}
