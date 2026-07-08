package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAccountUseCase {

    private final CoreAccountRepositoryPort accountRepository;

    public List<CoreAccount> getAllAccounts() {
        return accountRepository.findAll();
    }
}
