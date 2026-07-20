package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.DepositRequest;
import com.cooperativa.met.infrastructure.persistence.webhook.entity.ProcessedWebhookJpaEntity;
import com.cooperativa.met.infrastructure.persistence.webhook.repository.ProcessedWebhookJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessWebhookUseCase {

    private final ProcessedWebhookJpaRepository processedWebhookRepository;
    private final DepositUseCase depositUseCase;

    /**
     * Procesa un webhook de pago garantizando idempotencia estricta.
     * Al estar en un bloque transaccional y guardar primero el registro del webhook,
     * cualquier condición de carrera (Race Condition) resultará en una
     * DataIntegrityViolationException por llave primaria duplicada, abortando
     * el doble depósito instantáneamente.
     */
    @Transactional
    public void execute(String txId, String gateway, UUID userId, BigDecimal amountInPesos, String method) {
        log.info("Iniciando procesamiento transaccional del webhook {} para el usuario {}", txId, userId);

        if (processedWebhookRepository.existsById(txId)) {
            log.info("Transaction {} already processed (Idempotency). Ignoring.", txId);
            return;
        }

        // 1. Registrar intención de procesar para bloquear condiciones de carrera
        try {
            processedWebhookRepository.saveAndFlush(ProcessedWebhookJpaEntity.builder()
                    .transactionId(txId)
                    .gateway(gateway)
                    .processedAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.warn("Condición de carrera detectada. El webhook {} ya está siendo procesado por otro hilo.", txId);
            return;
        }

        // 2. Procesar el depósito
        DepositRequest request = new DepositRequest();
        request.setAmount(amountInPesos);
        request.setMethod(method);

        depositUseCase.execute(userId, request);
        
        log.info("Depósito de webhook procesado exitosamente para {}", txId);
    }
}
