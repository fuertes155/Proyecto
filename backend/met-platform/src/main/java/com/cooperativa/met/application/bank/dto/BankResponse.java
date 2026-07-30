package com.cooperativa.met.application.bank.dto;

import lombok.Builder;

@Builder
public record BankResponse(String code, String name, boolean supportsPse, boolean supportsPayout) {
}
