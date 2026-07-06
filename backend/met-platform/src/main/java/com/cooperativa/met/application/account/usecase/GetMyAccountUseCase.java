package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.CoreAccountResponse;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMyAccountUseCase {

    private final CoreAccountRepositoryPort accountRepository;

    @Transactional(readOnly = true)
    public CoreAccountResponse execute(UUID userId) {
        CoreAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una cuenta para el usuario"));

        return new CoreAccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getStatus().name()
        );
    }
}
