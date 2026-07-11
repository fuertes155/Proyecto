package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.GeneratePseLinkRequest;
import com.cooperativa.met.application.account.dto.GeneratePseLinkResponse;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GeneratePseLinkUseCase {

    private final CoreAccountRepositoryPort accountRepository;

    public GeneratePseLinkResponse execute(UUID userId, GeneratePseLinkRequest request) {
        if (request.getAmount() == null || request.getAmount().doubleValue() <= 0) {
            throw new BusinessRuleException("DEP_ERR_01", "El monto a recargar debe ser mayor a cero");
        }

        CoreAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessRuleException("DEP_ERR_02", "No tienes una cuenta activa para recargar"));

        if (!account.getStatus().name().equals("ACTIVE")) {
            throw new BusinessRuleException("DEP_ERR_03", "Tu cuenta no está activa");
        }

        // Mock Wompi/Bold PSE link generation
        String mockTransactionId = UUID.randomUUID().toString();
        String mockPaymentUrl = "https://mock.wompi.co/v1/payment/" + mockTransactionId + "?amount=" + request.getAmount() + "&userId=" + userId;

        return GeneratePseLinkResponse.builder()
                .paymentUrl(mockPaymentUrl)
                .transactionId(mockTransactionId)
                .build();
    }
}
