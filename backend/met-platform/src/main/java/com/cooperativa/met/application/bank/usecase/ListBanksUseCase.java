package com.cooperativa.met.application.bank.usecase;

import com.cooperativa.met.application.bank.dto.BankResponse;
import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListBanksUseCase {

    private final BankRepositoryPort bankRepository;

    public enum CatalogType { PSE, PAYOUT }

    public List<BankResponse> execute(CatalogType type) {
        return bankRepository.findAllActive().stream()
                .filter(bank -> type == CatalogType.PSE ? bank.isSupportsPse() : bank.isSupportsPayout())
                .map(bank -> BankResponse.builder()
                        .code(bank.getCode())
                        .name(bank.getName())
                        .supportsPse(bank.isSupportsPse())
                        .supportsPayout(bank.isSupportsPayout())
                        .build())
                .toList();
    }
}
