package com.cooperativa.met.domain.bank.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Cuenta bancaria externa registrada por el usuario para recibir payouts.
 *
 * IMPORTANTE: esta entidad NUNCA almacena el nombre/identificación del
 * titular. El sistema garantiza "solo cuentas propias" derivando siempre
 * esos datos de la identidad KYC del usuario dueño ({@code userId}) al
 * momento de ejecutar el payout, no de un campo editable aquí.
 *
 * Titularidad real: se verifica con un micro-depósito (ver
 * InitiateBankAccountVerificationUseCase) — {@code pendingVerificationAmount}
 * guarda el monto exacto enviado a la cuenta mientras se espera que el
 * usuario lo confirme; null significa que no hay una verificación en curso.
 */
@Getter
@Builder(toBuilder = true)
public class ExternalBankAccount {
    private final UUID id;
    private final UUID userId;
    private final String bankCode;
    private final BankAccountType accountType;
    private final String accountNumber;
    private final BankAccountVerificationStatus verificationStatus;
    private final Integer pendingVerificationAmount;
    private final int verificationAttempts;
    private final boolean active;
    private final Instant createdAt;
    private final Instant verifiedAt;

    public ExternalBankAccount withPendingVerificationAmount(int amount) {
        return this.toBuilder()
                .pendingVerificationAmount(amount)
                .verificationAttempts(0)
                .build();
    }

    public ExternalBankAccount withIncrementedAttempts() {
        return this.toBuilder()
                .verificationAttempts(this.verificationAttempts + 1)
                .build();
    }

    /** Limpia el intento en curso (se agotaron los intentos) — el usuario debe pedir un nuevo micro-depósito. */
    public ExternalBankAccount clearPendingVerification() {
        return this.toBuilder()
                .pendingVerificationAmount(null)
                .build();
    }

    public ExternalBankAccount verify() {
        return this.toBuilder()
                .verificationStatus(BankAccountVerificationStatus.VERIFIED)
                .pendingVerificationAmount(null)
                .verifiedAt(Instant.now())
                .build();
    }

    public ExternalBankAccount deactivate() {
        return this.toBuilder().active(false).build();
    }
}
