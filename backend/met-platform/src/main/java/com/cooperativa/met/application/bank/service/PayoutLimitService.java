package com.cooperativa.met.application.bank.service;

import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.admin.model.OperationLimit;
import com.cooperativa.met.domain.admin.port.OperationLimitRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Límites para EXTERNAL_PAYOUT, análogos a {@code TransferLimitService} pero
 * leídos de la tabla admin {@code operation_limits} (tipo_operacion =
 * 'EXTERNAL_PAYOUT') en vez de columnas por cuenta — el payout es un riesgo
 * distinto a la transferencia interna y merece un techo independiente,
 * configurable desde el panel admin sin desplegar código.
 *
 * Aplica un techo distinto según si la cuenta bancaria destino ya está
 * VERIFIED o sigue PENDING: mientras no exista un mecanismo real de
 * verificación de titularidad (ver RegisterExternalBankAccountUseCase),
 * las cuentas sin verificar operan bajo un límite mucho más conservador
 * ('EXTERNAL_PAYOUT_UNVERIFIED') — es una mitigación de riesgo mientras
 * se define esa pieza de negocio, no un reemplazo de ella.
 *
 * Los valores sembrados en la migración V30 son provisionales; deben ser
 * confirmados por el equipo de riesgo/cumplimiento antes de producción.
 */
@Service
@RequiredArgsConstructor
public class PayoutLimitService {

    private static final String OPERATION_TYPE_VERIFIED = "EXTERNAL_PAYOUT";
    private static final String OPERATION_TYPE_UNVERIFIED = "EXTERNAL_PAYOUT_UNVERIFIED";

    private final OperationLimitRepositoryPort operationLimitRepository;
    private final CoreTransactionRepositoryPort transactionRepository;

    public void validate(CoreAccount sourceAccount, BigDecimal amount, boolean destinationVerified) {
        String operationType = destinationVerified ? OPERATION_TYPE_VERIFIED : OPERATION_TYPE_UNVERIFIED;
        OperationLimit limit = operationLimitRepository.findByTipo(operationType)
                .orElseThrow(() -> new BusinessRuleException("PAYOUT_LIMIT_NOT_CONFIGURED",
                        "No hay un límite configurado para retiros a cuenta bancaria. Contacte a soporte."));

        if (!limit.isActivo()) {
            throw new BusinessRuleException("PAYOUT_DISABLED", "Los retiros a cuenta bancaria están deshabilitados temporalmente");
        }

        BigDecimal perTxLimit = BigDecimal.valueOf(limit.getMontoPorTransaccionMax());
        if (amount.compareTo(perTxLimit) > 0) {
            throw new BusinessRuleException("PER_TX_LIMIT_EXCEEDED",
                    String.format("El monto excede el límite por transacción de $%s", perTxLimit));
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Bogota"));
        Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS).toInstant();
        Instant endOfDay = now.truncatedTo(ChronoUnit.DAYS).plusDays(1).toInstant();

        BigDecimal sumToday = transactionRepository.sumOutgoingByAccountIdAndTypeAndDateRange(
                sourceAccount.getId(), TransactionType.EXTERNAL_PAYOUT, startOfDay, endOfDay);

        BigDecimal dailyLimit = BigDecimal.valueOf(limit.getMontoDiarioMax());
        BigDecimal projectedTotal = sumToday.add(amount);

        if (projectedTotal.compareTo(dailyLimit) > 0) {
            throw new BusinessRuleException("DAILY_LIMIT_EXCEEDED",
                    String.format("El retiro excede tu límite diario de $%s. Ya has retirado $%s hoy.", dailyLimit, sumToday));
        }
    }
}
