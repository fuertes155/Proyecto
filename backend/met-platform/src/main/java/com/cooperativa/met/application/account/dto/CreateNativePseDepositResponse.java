package com.cooperativa.met.application.account.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateNativePseDepositResponse {
    private String transactionId;
    private String asyncPaymentUrl;
    private String status;
}
