package com.cooperativa.met.application.compliance.service;

import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.compliance.model.AlertSeverity;
import com.cooperativa.met.domain.compliance.model.AlertStatus;
import com.cooperativa.met.domain.compliance.model.AlertType;
import com.cooperativa.met.domain.compliance.model.ComplianceAlert;
import com.cooperativa.met.domain.compliance.port.ComplianceAlertRepositoryPort;
import com.cooperativa.met.infrastructure.config.MetComplianceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Control SARLAFT de "operaciones inusuales": monto, frecuencia y patrones
 * atípicos. A propósito NUNCA bloquea ni lanza excepción hacia el llamador —
 * a diferencia de {@code FraudDetectionService} (que sí rechaza la operación),
 * el monitoreo SARLAFT deja pasar la operación y la deja registrada para que
 * un oficial de cumplimiento la revise después (esa es la naturaleza de este
 * control: detectar y reportar, no impedir).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SarlaftAlertEngine {

    private final CoreTransactionRepositoryPort transactionRepository;
    private final ComplianceAlertRepositoryPort alertRepository;
    private final MetComplianceProperties complianceProperties;

    public void evaluate(UUID userId, UUID accountId, UUID transactionId, TransactionType type, BigDecimal amount) {
        try {
            doEvaluate(userId, accountId, transactionId, type, amount);
        } catch (Exception e) {
            log.error("Error evaluando reglas SARLAFT para userId={} transactionId={} (la operación de dinero NO se ve afectada)",
                    userId, transactionId, e);
        }
    }

    private void doEvaluate(UUID userId, UUID accountId, UUID transactionId, TransactionType type, BigDecimal amount) {
        MetComplianceProperties.Sarlaft rules = complianceProperties.getSarlaft();
        Instant now = Instant.now();
        Instant since24h = now.minus(Duration.ofHours(24));

        // findByAccountId trae todo el historial de la cuenta; para el volumen de
        // este sistema es aceptable filtrar en memoria. Si el volumen crece mucho,
        // conviene una consulta acotada por fecha directamente en BD.
        List<CoreTransaction> history = transactionRepository.findByAccountId(accountId);
        List<CoreTransaction> last24h = history.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(since24h))
                .toList();

        List<ComplianceAlert> triggered = new ArrayList<>();

        BigDecimal threshold = rules.getUnusualAmountThreshold();
        boolean overAbsoluteThreshold = amount.compareTo(threshold) > 0;
        BigDecimal historicalAverage = average(history, transactionId);
        boolean overHistoricalMultiple = historicalAverage.compareTo(BigDecimal.ZERO) > 0
                && amount.compareTo(historicalAverage.multiply(rules.getUnusualAmountMultiplier())) > 0;

        if (overAbsoluteThreshold || overHistoricalMultiple) {
            triggered.add(buildAlert(userId, transactionId, AlertType.UNUSUAL_AMOUNT,
                    overAbsoluteThreshold ? AlertSeverity.HIGH : AlertSeverity.MEDIUM,
                    overAbsoluteThreshold
                            ? String.format("Operación de $%,.2f supera el umbral SARLAFT de $%,.2f.", amount, threshold)
                            : String.format("Operación de $%,.2f es %sx el promedio histórico de esta cuenta ($%,.2f).",
                                    amount, rules.getUnusualAmountMultiplier(), historicalAverage)));
        }

        int countIncludingCurrent = last24h.size() + 1;
        if (countIncludingCurrent > rules.getMaxTransactionsPerDay()) {
            triggered.add(buildAlert(userId, transactionId, AlertType.UNUSUAL_FREQUENCY, AlertSeverity.MEDIUM,
                    String.format("%d operaciones en las últimas 24 horas (límite de referencia: %d).",
                            countIncludingCurrent, rules.getMaxTransactionsPerDay())));
        }

        BigDecimal structuringLowerBound = threshold.multiply(BigDecimal.valueOf(rules.getStructuringLowerRatio()));
        long nearThresholdCount = last24h.stream()
                .filter(t -> isBetween(t.getAmount(), structuringLowerBound, threshold))
                .count();
        if (isBetween(amount, structuringLowerBound, threshold)) {
            nearThresholdCount++;
        }
        if (nearThresholdCount >= rules.getStructuringMinOccurrences()) {
            triggered.add(buildAlert(userId, transactionId, AlertType.STRUCTURING_PATTERN, AlertSeverity.HIGH,
                    String.format("%d operaciones en 24h entre $%,.2f y $%,.2f — justo debajo del umbral, patrón típico de fraccionamiento.",
                            nearThresholdCount, structuringLowerBound, threshold)));
        }

        if (type == TransactionType.TRANSFER || type == TransactionType.EXTERNAL_PAYOUT || type == TransactionType.WITHDRAWAL) {
            Instant windowStart = now.minus(Duration.ofMinutes(rules.getRapidInOutWindowMinutes()));
            boolean rapidInOut = history.stream().anyMatch(t ->
                    t.getType() == TransactionType.DEPOSIT
                            && t.getCreatedAt() != null && t.getCreatedAt().isAfter(windowStart)
                            && isSimilarAmount(t.getAmount(), amount));
            if (rapidInOut) {
                triggered.add(buildAlert(userId, transactionId, AlertType.RAPID_IN_OUT, AlertSeverity.HIGH,
                        String.format("Salida de $%,.2f dentro de los %d minutos siguientes a un depósito de monto similar.",
                                amount, rules.getRapidInOutWindowMinutes())));
            }
        }

        if (!triggered.isEmpty()) {
            log.warn("SARLAFT: {} alerta(s) generadas para userId={} transactionId={}", triggered.size(), userId, transactionId);
            triggered.forEach(alertRepository::save);
        }
    }

    private BigDecimal average(List<CoreTransaction> history, UUID excludingTransactionId) {
        List<CoreTransaction> prior = history.stream()
                .filter(t -> !t.getId().equals(excludingTransactionId))
                .toList();
        if (prior.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = prior.stream().map(CoreTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(prior.size()), 2, RoundingMode.HALF_UP);
    }

    private boolean isBetween(BigDecimal amount, BigDecimal lower, BigDecimal upperExclusive) {
        return amount.compareTo(lower) >= 0 && amount.compareTo(upperExclusive) < 0;
    }

    /** Montos "similares" si difieren en menos de un 10%. */
    private boolean isSimilarAmount(BigDecimal a, BigDecimal b) {
        if (a == null || b == null || a.compareTo(BigDecimal.ZERO) == 0) return false;
        BigDecimal diff = a.subtract(b).abs();
        BigDecimal tolerance = a.multiply(new BigDecimal("0.10"));
        return diff.compareTo(tolerance) <= 0;
    }

    private ComplianceAlert buildAlert(UUID userId, UUID transactionId, AlertType type, AlertSeverity severity, String description) {
        return ComplianceAlert.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .transactionId(transactionId)
                .alertType(type)
                .severity(severity)
                .description(description)
                .status(AlertStatus.OPEN)
                .createdAt(Instant.now())
                .build();
    }
}
