package com.cooperativa.met.application.savings.dto;

import com.cooperativa.met.domain.savings.model.WithdrawalType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class WithdrawSavingsRequest {
    private UUID accountId;
    private BigDecimal amount;
    private WithdrawalType withdrawalType;
}
