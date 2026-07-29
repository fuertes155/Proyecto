package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.RecentRecipientResponse;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetRecentRecipientsUseCase {

    private static final int MAX_RECENT_RECIPIENTS = 5;

    private final CoreAccountRepositoryPort accountRepository;
    private final CoreTransactionRepositoryPort transactionRepository;
    private final UserRepositoryPort userRepository;

    @Transactional(readOnly = true)
    public List<RecentRecipientResponse> execute(UUID userId) {
        CoreAccount myAccount = accountRepository.findByUserId(userId).orElse(null);
        if (myAccount == null) {
            return List.of();
        }

        List<CoreTransaction> transactions = transactionRepository.findByAccountId(myAccount.getId());
        transactions.sort(Comparator.comparing(CoreTransaction::getCreatedAt).reversed());

        // Destinos únicos de transferencias que YO envié, en orden del más reciente al más antiguo
        LinkedHashSet<UUID> destinationIds = new LinkedHashSet<>();
        for (CoreTransaction tx : transactions) {
            if (tx.getType() == TransactionType.TRANSFER && myAccount.getId().equals(tx.getSourceAccountId())) {
                destinationIds.add(tx.getDestinationAccountId());
            }
        }

        List<RecentRecipientResponse> result = new ArrayList<>();
        for (UUID destinationId : destinationIds) {
            if (result.size() >= MAX_RECENT_RECIPIENTS) {
                break;
            }
            accountRepository.findById(destinationId).ifPresent(destAccount ->
                    userRepository.findById(destAccount.getUserId()).ifPresent(owner ->
                            result.add(new RecentRecipientResponse(
                                    destAccount.getId(),
                                    destAccount.getAccountNumber(),
                                    owner.getFirstName() + " " + owner.getLastName()
                            ))
                    )
            );
        }
        return result;
    }
}
