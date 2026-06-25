package com.cooperativa.met.infrastructure.persistence.admin.adapter;

import com.cooperativa.met.domain.admin.model.MaintenanceWindow;
import com.cooperativa.met.domain.admin.port.MaintenanceRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.admin.entity.MaintenanceWindowJpaEntity;
import com.cooperativa.met.infrastructure.persistence.admin.repository.MaintenanceWindowJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MaintenanceWindowAdapter implements MaintenanceRepositoryPort {

    private final MaintenanceWindowJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<MaintenanceWindow> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<MaintenanceWindow> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<MaintenanceWindow> findActiva() {
        return jpaRepository.findByActivoTrue().map(this::toDomain);
    }

    @Override
    public MaintenanceWindow save(MaintenanceWindow window) {
        return toDomain(jpaRepository.save(toEntity(window)));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private MaintenanceWindow toDomain(MaintenanceWindowJpaEntity e) {
        List<String> endpoints = new ArrayList<>();
        if (e.getEndpointsActivos() != null) {
            try {
                endpoints = objectMapper.readValue(e.getEndpointsActivos(), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }
        return MaintenanceWindow.builder()
                .id(e.getId())
                .descripcion(e.getDescripcion())
                .inicio(e.getInicio())
                .fin(e.getFin())
                .activo(e.isActivo())
                .endpointsActivos(endpoints)
                .creadoPor(e.getCreadoPor())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private MaintenanceWindowJpaEntity toEntity(MaintenanceWindow w) {
        MaintenanceWindowJpaEntity e = new MaintenanceWindowJpaEntity();
        e.setId(w.getId() != null ? w.getId() : UUID.randomUUID());
        e.setDescripcion(w.getDescripcion());
        e.setInicio(w.getInicio());
        e.setFin(w.getFin());
        e.setActivo(w.isActivo());
        e.setCreadoPor(w.getCreadoPor());
        e.setCreatedAt(w.getCreatedAt() != null ? w.getCreatedAt() : Instant.now());
        if (w.getEndpointsActivos() != null) {
            try {
                e.setEndpointsActivos(objectMapper.writeValueAsString(w.getEndpointsActivos()));
            } catch (Exception ignored) {}
        }
        return e;
    }
}
