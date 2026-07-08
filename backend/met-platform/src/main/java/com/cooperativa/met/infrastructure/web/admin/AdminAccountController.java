package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.account.usecase.AdminAccountUseCase;
import com.cooperativa.met.domain.account.model.CoreAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountUseCase adminAccountUseCase;

    @GetMapping
    public ResponseEntity<List<CoreAccount>> getAllAccounts() {
        List<CoreAccount> accounts = adminAccountUseCase.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }
}
