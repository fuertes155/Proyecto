package com.cooperativa.met.infrastructure.persistence.admin.adapter;

import com.cooperativa.met.domain.admin.model.PlatformRevenue;
import com.cooperativa.met.domain.admin.port.PlatformRevenuePort;
import com.cooperativa.met.infrastructure.persistence.admin.entity.PlatformRevenueJpaEntity;
import com.cooperativa.met.infrastructure.persistence.admin.repository.PlatformRevenueJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformRevenueAdapter implements PlatformRevenuePort {

    private final PlatformRevenueJpaRepository repository;

    @Override
    public PlatformRevenue save(PlatformRevenue revenue) {
        PlatformRevenueJpaEntity entity = PlatformRevenueJpaEntity.builder()
                .id(revenue.getId())
                .userId(revenue.getUserId())
                .amount(revenue.getAmount())
                .description(revenue.getDescription())
                .source(revenue.getSource())
                .createdAt(revenue.getCreatedAt())
                .build();
        
        PlatformRevenueJpaEntity saved = repository.save(entity);
        
        return PlatformRevenue.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .amount(saved.getAmount())
                .description(saved.getDescription())
                .source(saved.getSource())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
