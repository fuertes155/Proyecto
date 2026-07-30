package com.cooperativa.met.application.account.service;

import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Reversión de débitos de payout con reintento ante conflictos de bloqueo
 * optimista (dos escrituras concurrentes sobre la misma cuenta). Se usa
 * cuando un payout externo es rechazado después de haber debitado la
 * wallet — dejar de revertir el dinero por un conflicto de concurrencia
 * sería plata perdida para el usuario, así que aquí sí vale la pena
 * reintentar en vez de fallar en el primer intento.
 *
 * El payout solo debita {@code interestBalance} (ganancias) — nunca
 * {@code principalBalance} (capital) — así que la reversión acredita al
 * mismo bucket del que salió el dinero.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountReversalService {

    private static final int MAX_ATTEMPTS = 3;

    private final CoreAccountRepositoryPort accountRepository;

    /**
     * Acredita {@code amount} a la cuenta {@code accountId}, reintentando
     * hasta {@value #MAX_ATTEMPTS} veces si otra escritura concurrente
     * invalida el bloqueo optimista.
     *
     * @throws OptimisticLockingFailureException si se agotan los intentos —
     *         el llamador debe tratarlo como "requiere reconciliación manual".
     */
    public CoreAccount creditWithRetry(UUID accountId, BigDecimal amount) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                CoreAccount account = accountRepository.findById(accountId)
                        .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta " + accountId));
                return accountRepository.save(account.creditInterest(amount));
            } catch (OptimisticLockingFailureException e) {
                log.warn("Conflicto de bloqueo optimista revirtiendo {} a la cuenta {} (intento {}/{})",
                        amount, accountId, attempt, MAX_ATTEMPTS);
                if (attempt == MAX_ATTEMPTS) {
                    log.error("No fue posible revertir {} a la cuenta {} tras {} intentos — requiere reconciliación manual",
                            amount, accountId, MAX_ATTEMPTS);
                    throw e;
                }
            }
        }
        throw new IllegalStateException("unreachable");
    }
}
