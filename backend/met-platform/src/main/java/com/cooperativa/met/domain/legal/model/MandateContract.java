package com.cooperativa.met.domain.legal.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class MandateContract {
    private final UUID id;
    private final UUID userId;
    private final String documentNumber;
    private final byte[] pdfContent;
    private final String pdfHashSha256;
    private final Instant signedAt;
    private final String ipAddress;
    private final String userAgent;
    private final String otpTransactionId;
    private final MandateContractStatus status;

    public MandateContract withStatus(MandateContractStatus newStatus) {
        return toBuilder().status(newStatus).build();
    }
}
