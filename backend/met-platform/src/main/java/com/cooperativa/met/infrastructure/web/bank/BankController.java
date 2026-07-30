package com.cooperativa.met.infrastructure.web.bank;

import com.cooperativa.met.application.bank.dto.BankResponse;
import com.cooperativa.met.application.bank.usecase.ListBanksUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/banks")
@RequiredArgsConstructor
public class BankController {

    private final ListBanksUseCase listBanksUseCase;

    @GetMapping
    public ResponseEntity<List<BankResponse>> list(
            @RequestParam(defaultValue = "PAYOUT") ListBanksUseCase.CatalogType type) {
        return ResponseEntity.ok(listBanksUseCase.execute(type));
    }
}
