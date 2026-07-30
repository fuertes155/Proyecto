package com.cooperativa.met.domain.bank.port;

import com.cooperativa.met.domain.bank.model.Bank;

import java.util.List;
import java.util.Optional;

public interface BankRepositoryPort {
    List<Bank> findAllActive();
    Optional<Bank> findByCode(String code);
    Bank save(Bank bank);
}
