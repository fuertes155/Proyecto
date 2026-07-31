package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.lending.usecase.ProcessLoanCollectionsUseCase;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Utilidades exclusivas de desarrollo para probar manualmente flujos que en
 * producción solo se disparan por cron (ej: {@code LoanCollectionJob} corre
 * a las 00:01 hora Bogotá) o que hoy no tienen ningún flujo real que los
 * complete (ej: no existe un "agregar tarjeta" que guarde paymentCardToken).
 * Solo activo con {@code SPRING_PROFILES_ACTIVE=dev} — nunca existe en producción.
 */
@RestController
@RequestMapping("/v1/admin/dev")
@Profile("dev")
@RequiredArgsConstructor
public class DevSimulationController {

    private final ProcessLoanCollectionsUseCase processLoanCollectionsUseCase;
    private final EncryptionPort encryptionPort;

    /** Dispara manualmente la cobranza de cuotas (equivalente a esperar al cron de las 00:01). */
    @PostMapping("/trigger-collections")
    public ResponseEntity<Map<String, String>> triggerCollections() {
        processLoanCollectionsUseCase.execute();
        return ResponseEntity.ok(Map.of("message", "Cobranza ejecutada manualmente"));
    }

    /**
     * Cifra un texto plano con el mismo AES-GCM que usan los campos @Convert(StandardCryptoConverter),
     * para poder sembrar datos de prueba válidos (ej: payment_card_token) sin exponer la llave AES.
     */
    @PostMapping("/encrypt")
    public ResponseEntity<Map<String, String>> encrypt(@RequestBody Map<String, String> body) {
        String plainText = body.get("plainText");
        return ResponseEntity.ok(Map.of("cipherText", encryptionPort.encrypt(plainText)));
    }
}
