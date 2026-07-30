package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.bank.usecase.SyncBankCatalogUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/admin/banks")
@RequiredArgsConstructor
public class BankCatalogAdminController {

    private final SyncBankCatalogUseCase syncBankCatalogUseCase;

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Integer>> sync() {
        int updated = syncBankCatalogUseCase.execute();
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
