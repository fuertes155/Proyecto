package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.CreateNativePseDepositRequest;
import com.cooperativa.met.application.account.dto.CreateNativePseDepositResponse;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.bank.model.Bank;
import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import com.cooperativa.met.domain.bank.port.PseGatewayPort;
import com.cooperativa.met.domain.bank.port.PseTransactionRequest;
import com.cooperativa.met.domain.bank.port.PseTransactionResult;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Crea un depósito PSE nativo: el usuario elige el banco DENTRO de la app
 * (a partir del catálogo real sincronizado desde Wompi) y el backend crea
 * la transacción directamente vía API, en vez de redirigir al checkout
 * hosteado donde Wompi mostraría de nuevo la lista de bancos.
 *
 * Reutiliza el mismo formato de referencia que {@link GeneratePseLinkUseCase}
 * ("MET-{userId}-{timestamp}") para que el webhook existente
 * ({@code WebhookController}/{@code ProcessWebhookUseCase}) procese este
 * depósito sin ningún cambio — solo cambia cómo se INICIA el pago, no cómo
 * se confirma.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateNativePseDepositUseCase {

    private final CoreAccountRepositoryPort accountRepository;
    private final UserRepositoryPort userRepository;
    private final BankRepositoryPort bankRepository;
    private final PseGatewayPort pseGatewayPort;

    public CreateNativePseDepositResponse execute(UUID userId, CreateNativePseDepositRequest request) {
        CoreAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessRuleException("DEP_ERR_02", "No tienes una cuenta activa para recargar"));
        if (!account.getStatus().name().equals("ACTIVE")) {
            throw new BusinessRuleException("DEP_ERR_03", "Tu cuenta no está activa");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Bank bank = bankRepository.findByCode(request.getBankCode())
                .orElseThrow(() -> new BusinessRuleException("BANK_NOT_FOUND", "El banco seleccionado no existe"));
        if (!bank.isActive() || !bank.isSupportsPse()) {
            throw new BusinessRuleException("BANK_NOT_SUPPORTED", "El banco seleccionado no soporta PSE por el momento");
        }
        if (bank.getWompiPseCode() == null || bank.getWompiPseCode().isBlank()) {
            throw new BusinessRuleException("BANK_NOT_SYNCED", "Este banco aún no está disponible para PSE, intenta más tarde");
        }

        String reference = "MET-" + userId + "-" + System.currentTimeMillis();

        PseTransactionResult result = pseGatewayPort.createPseTransaction(new PseTransactionRequest(
                reference,
                request.getAmount(),
                user.getEmail(),
                user.getDocumentType().name(),
                user.getDocumentNumber(),
                bank.getWompiPseCode(),
                buildRedirectUrl(userId, request.getReturnUrl()),
                "Recarga MET"));

        log.info("Transacción PSE nativa creada para usuario {} banco {} reference {}", userId, bank.getCode(), reference);

        return CreateNativePseDepositResponse.builder()
                .transactionId(result.transactionId())
                .asyncPaymentUrl(result.asyncPaymentUrl())
                .status(result.status())
                .build();
    }

    private String buildRedirectUrl(UUID userId, String returnUrl) {
        if (returnUrl != null && !returnUrl.isBlank()
                && (returnUrl.startsWith("https://met-platform.com") || returnUrl.startsWith("metapp://"))) {
            return returnUrl;
        }
        return "metapp://deposit/success?userId=" + userId;
    }
}
