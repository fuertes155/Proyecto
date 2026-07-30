package com.cooperativa.met.domain.bank.port;

public record PayoutGatewayResult(String railReference, PayoutGatewayStatus status, String failureCode, String failureMessage) {

    public enum PayoutGatewayStatus {
        ACCEPTED,
        REJECTED
    }
}
