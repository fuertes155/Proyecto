package com.cooperativa.met.infrastructure.persistence.bank.entity;

import com.cooperativa.met.domain.bank.model.Bank;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "banks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankJpaEntity {

    @Id
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "wompi_bank_id")
    private String wompiBankId;

    @Column(name = "wompi_pse_code")
    private String wompiPseCode;

    @Column(name = "supports_pse", nullable = false)
    private boolean supportsPse;

    @Column(name = "supports_payout", nullable = false)
    private boolean supportsPayout;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Bank toDomain() {
        return Bank.builder()
                .code(code)
                .name(name)
                .wompiBankId(wompiBankId)
                .wompiPseCode(wompiPseCode)
                .supportsPse(supportsPse)
                .supportsPayout(supportsPayout)
                .active(active)
                .updatedAt(updatedAt)
                .build();
    }

    public static BankJpaEntity fromDomain(Bank domain) {
        return BankJpaEntity.builder()
                .code(domain.getCode())
                .name(domain.getName())
                .wompiBankId(domain.getWompiBankId())
                .wompiPseCode(domain.getWompiPseCode())
                .supportsPse(domain.isSupportsPse())
                .supportsPayout(domain.isSupportsPayout())
                .active(domain.isActive())
                .updatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : Instant.now())
                .build();
    }
}
