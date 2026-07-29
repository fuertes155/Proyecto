package com.cooperativa.met.application.account.dto;

import java.util.UUID;

public record RecentRecipientResponse(
        UUID accountId,
        String accountNumber,
        String ownerName
) {
}
