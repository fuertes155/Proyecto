package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.VerifyRecipientResponse;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerifyRecipientUseCase {

    private final CoreAccountRepositoryPort accountRepository;
    private final UserRepositoryPort userRepository;

    @Transactional(readOnly = true)
    public VerifyRecipientResponse execute(String identifier, UUID currentUserId) {
        // Try finding by account number first
        Optional<CoreAccount> accountOpt = accountRepository.findByAccountNumber(identifier);
        
        if (accountOpt.isEmpty()) {
            // If not found by account number, try by phone
            Optional<User> userOpt = userRepository.findByPhone(identifier);
            if (userOpt.isPresent()) {
                accountOpt = accountRepository.findByUserId(userOpt.get().getId());
            }
        }

        CoreAccount account = accountOpt
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ninguna cuenta con el número o celular ingresado"));

        if (account.getUserId().equals(currentUserId)) {
            throw new com.cooperativa.met.domain.common.exception.BusinessRuleException("SAME_ACCOUNT", "No puedes enviarte dinero a ti mismo a través de esta opción");
        }

        User owner = userRepository.findById(account.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el propietario de la cuenta"));

        // Format name to show (e.g. "Juan Perez")
        String fullName = owner.getFirstName() + " " + owner.getLastName();

        return new VerifyRecipientResponse(account.getId(), fullName);
    }
}
