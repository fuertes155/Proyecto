package com.cooperativa.met.domain.bank.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder(toBuilder = true)
public class Bank {
    private final String code;
    private final String name;
    /** Código de banco en el producto "Pagos a Terceros" (payout) de Wompi. */
    private final String wompiBankId;
    /** Código de institución financiera PSE — namespace distinto al de payout, mismo Wompi. */
    private final String wompiPseCode;
    private final boolean supportsPse;
    private final boolean supportsPayout;
    private final boolean active;
    private final Instant updatedAt;

    public Bank withPayoutMapping(String wompiBankId, boolean supportsPayout) {
        return this.toBuilder()
                .wompiBankId(wompiBankId)
                .supportsPayout(supportsPayout)
                .updatedAt(Instant.now())
                .build();
    }

    public Bank withPseMapping(String wompiPseCode) {
        return this.toBuilder()
                .wompiPseCode(wompiPseCode)
                .supportsPse(true)
                .updatedAt(Instant.now())
                .build();
    }
}
