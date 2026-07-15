package com.cooperativa.met.infrastructure.persistence.legal.adapter;

import com.cooperativa.met.domain.legal.model.MandateContract;
import com.cooperativa.met.domain.legal.model.MandateContractStatus;
import com.cooperativa.met.domain.legal.port.MandateContractRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.legal.entity.MandateContractJpaEntity;
import com.cooperativa.met.infrastructure.persistence.legal.repository.MandateContractJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MandateContractAdapter implements MandateContractRepositoryPort {

    private final MandateContractJpaRepository repository;

    @Override
    public MandateContract save(MandateContract contract) {
        return toModel(repository.save(toEntity(contract)));
    }

    @Override
    public Optional<MandateContract> findById(UUID id) {
        return repository.findById(id).map(this::toModel);
    }

    @Override
    public Optional<MandateContract> findByUserId(UUID userId) {
        return repository.findByUserId(userId).map(this::toModel);
    }

    private MandateContractJpaEntity toEntity(MandateContract model) {
        return MandateContractJpaEntity.builder()
                .id(model.getId())
                .userId(model.getUserId())
                .documentNumber(model.getDocumentNumber())
                .pdfContent(model.getPdfContent())
                .pdfHashSha256(model.getPdfHashSha256())
                .signedAt(model.getSignedAt())
                .ipAddress(model.getIpAddress())
                .userAgent(model.getUserAgent())
                .otpTransactionId(model.getOtpTransactionId())
                .status(model.getStatus().name())
                .build();
    }

    private MandateContract toModel(MandateContractJpaEntity entity) {
        return MandateContract.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .documentNumber(entity.getDocumentNumber())
                .pdfContent(entity.getPdfContent())
                .pdfHashSha256(entity.getPdfHashSha256())
                .signedAt(entity.getSignedAt())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .otpTransactionId(entity.getOtpTransactionId())
                .status(MandateContractStatus.valueOf(entity.getStatus()))
                .build();
    }
}
