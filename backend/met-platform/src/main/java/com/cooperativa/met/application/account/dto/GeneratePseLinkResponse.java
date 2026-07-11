package com.cooperativa.met.application.account.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeneratePseLinkResponse {
    private String paymentUrl;
    private String transactionId;
}
