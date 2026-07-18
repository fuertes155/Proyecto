package com.cooperativa.met.application.account.service;

import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferLimitService {

    private final CoreTransactionRepositoryPort transactionRepository;

    /**
     * Valida que una nueva transferencia no exceda los límites permitidos.
     * 1. Límite por transacción
     * 2. Límite diario sumado a las transacciones anteriores de hoy
     */
    public void validateTransferLimits(CoreAccount sourceAccount, BigDecimal amountToTransfer) {
        // 1. Validar límite por transacción
        if (amountToTransfer.compareTo(sourceAccount.getPerTransactionLimit()) > 0) {
            throw new BusinessRuleException("PER_TX_LIMIT_EXCEEDED", 
                    String.format("El monto excede el límite por transacción de $%s", sourceAccount.getPerTransactionLimit()));
        }

        // 2. Calcular límites diarios (ventana 00:00:00 a 23:59:59 hoy, en UTC o la zona configurada)
        // Usamos UTC por simplicidad, pero podría ser America/Bogota según la región del usuario.
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Bogota"));
        Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS).toInstant();
        Instant endOfDay = now.truncatedTo(ChronoUnit.DAYS).plusDays(1).toInstant();

        BigDecimal sumToday = transactionRepository.sumOutgoingTransfersByAccountIdAndDateRange(
                sourceAccount.getId(), startOfDay, endOfDay);

        BigDecimal projectedTotal = sumToday.add(amountToTransfer);

        if (projectedTotal.compareTo(sourceAccount.getDailyTransferLimit()) > 0) {
            throw new BusinessRuleException("DAILY_LIMIT_EXCEEDED", 
                    String.format("La transferencia excede tu límite diario de $%s. Ya has transferido $%s hoy.", 
                            sourceAccount.getDailyTransferLimit(), sumToday));
        }

        // 3. Bloqueo Nocturno Anti-Secuestro Exprés (Bóveda de Tiempo)
        // Ejemplo: Entre las 00:00 y las 06:00, las transferencias mayores a $200,000 están bloqueadas
        int currentHour = now.getHour();
        if (currentHour >= 0 && currentHour < 6) {
            BigDecimal vaultLimit = new BigDecimal("200000");
            if (amountToTransfer.compareTo(vaultLimit) > 0) {
                log.warn("Transferencia bloqueada por Bóveda de Tiempo. Cuenta: {}, Monto: {}", sourceAccount.getId(), amountToTransfer);
                throw new BusinessRuleException("TIME_VAULT_ACTIVE", 
                        "Por tu seguridad, el 'Modo Bóveda' está activo entre 12:00 AM y 6:00 AM. No se permiten transferencias mayores a $200,000 en este horario.");
            }
        }
    }
}
