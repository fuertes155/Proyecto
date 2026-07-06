package com.cooperativa.met.application.account.dto;

import java.util.UUID;

public record VerifyRecipientResponse(
        UUID accountId,
        String ownerName
) {
}
