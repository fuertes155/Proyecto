package com.cooperativa.met.application.bank.usecase;

import com.cooperativa.met.domain.bank.model.Bank;
import com.cooperativa.met.domain.bank.model.BankAccountVerificationStatus;
import com.cooperativa.met.domain.bank.model.ExternalBankAccount;
import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import com.cooperativa.met.domain.bank.port.ExternalBankAccountRepositoryPort;
import com.cooperativa.met.domain.bank.port.PayoutGatewayPort;
import com.cooperativa.met.domain.bank.port.PayoutGatewayRequest;
import com.cooperativa.met.domain.bank.port.PayoutGatewayResult;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * Envía un micro-depósito de monto aleatorio a la cuenta bancaria externa
 * recién registrada, para que el usuario lo confirme en
 * {@link ConfirmBankAccountVerificationUseCase} y así probar que tiene
 * acceso real a esa cuenta (titularidad).
 *
 * El micro-depósito lo fondea la cuenta operativa de la plataforma
 * (met.wompi.payouts.source-account-id) — NO se debita del saldo del
 * usuario, es un costo operativo de verificación, igual que en otras
 * fintechs (Stripe/Plaid usan el mismo patrón donde no hay una API de
 * consulta de titularidad directa).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InitiateBankAccountVerificationUseCase {

    /** Rango amplio y no redondo para que sea inconfundible en el extracto bancario. */
    private static final int MIN_AMOUNT = 1000;
    private static final int MAX_AMOUNT = 4900;

    private final ExternalBankAccountRepositoryPort externalBankAccountRepository;
    private final BankRepositoryPort bankRepository;
    private final UserRepositoryPort userRepository;
    private final PayoutGatewayPort payoutGatewayPort;
    private final SecureRandom secureRandom = new SecureRandom();

    /** Solo dev/local: imprime el monto del micro-depósito en logs, ya que sin un
     *  banco real el usuario no puede verlo en su extracto para confirmarlo. */
    @org.springframework.beans.factory.annotation.Value("${met.dev.log-verification-amount:false}")
    private boolean logVerificationAmount;

    public void execute(UUID userId, UUID externalBankAccountId) {
        ExternalBankAccount account = externalBankAccountRepository.findById(externalBankAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada"));

        if (!account.getUserId().equals(userId)) {
            throw new BusinessRuleException("FORBIDDEN", "Esta cuenta bancaria no te pertenece");
        }
        if (account.getVerificationStatus() == BankAccountVerificationStatus.VERIFIED) {
            throw new BusinessRuleException("ALREADY_VERIFIED", "Esta cuenta ya está verificada");
        }

        Bank bank = bankRepository.findByCode(account.getBankCode())
                .orElseThrow(() -> new BusinessRuleException("BANK_NOT_FOUND", "Banco no encontrado"));
        if (bank.getWompiBankId() == null || bank.getWompiBankId().isBlank()) {
            throw new BusinessRuleException("BANK_NOT_SYNCED", "Este banco aún no está disponible para verificación");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        int amount = MIN_AMOUNT + secureRandom.nextInt(MAX_AMOUNT - MIN_AMOUNT + 1);
        String reference = "VERIFY-" + account.getId() + "-" + System.currentTimeMillis();

        if (logVerificationAmount) {
            log.warn("=================================================");
            log.warn("[DEV] Micro-depósito de verificación para la cuenta {}: monto = {} (centavos)", account.getId(), amount);
            log.warn("      Confírmalo en la app con ese valor.");
            log.warn("=================================================");
        }

        PayoutGatewayResult result = payoutGatewayPort.initiatePayout(new PayoutGatewayRequest(
                reference,
                BigDecimal.valueOf(amount),
                bank.getWompiBankId(),
                account.getAccountType().name(),
                account.getAccountNumber(),
                user.getDocumentType().name(),
                user.getDocumentNumber(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                account.getId()));

        if (result.status() != PayoutGatewayResult.PayoutGatewayStatus.ACCEPTED) {
            log.warn("No fue posible enviar el depósito de verificación para la cuenta {}: {}",
                    account.getId(), result.failureMessage());
            throw new BusinessRuleException("VERIFICATION_SEND_FAILED",
                    "No fue posible enviar el depósito de verificación. Intenta de nuevo más tarde.");
        }

        externalBankAccountRepository.save(account.withPendingVerificationAmount(amount));
        log.info("Depósito de verificación enviado a la cuenta {} (railReference={})", account.getId(), result.railReference());
    }
}
