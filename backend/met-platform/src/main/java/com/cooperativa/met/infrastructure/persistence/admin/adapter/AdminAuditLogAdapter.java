package com.cooperativa.met.infrastructure.persistence.admin.adapter;

import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.infrastructure.persistence.admin.entity.AdminAuditLogJpaEntity;
import com.cooperativa.met.infrastructure.persistence.admin.repository.AdminAuditLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AdminAuditLogAdapter implements AdminAuditLogPort {

    private final AdminAuditLogJpaRepository jpaRepository;

    @Override
    public void log(AdminAuditEntry entry) {
        AdminAuditLogJpaEntity e = new AdminAuditLogJpaEntity();
        e.setId(entry.getId() != null ? entry.getId() : UUID.randomUUID());
        e.setActorAdminId(entry.getActorAdminId());
        e.setAccion(entry.getAccion());
        e.setEntidadAfectada(entry.getEntidadAfectada());
        e.setIdEntidad(entry.getIdEntidad());
        e.setValoresAnteriores(entry.getValoresAnteriores());
        e.setValoresNuevos(entry.getValoresNuevos());
        e.setMotivo(entry.getMotivo());
        e.setIpOrigen(entry.getIpOrigen());
        e.setTimestamp(entry.getTimestamp());
        jpaRepository.save(e);
    }

    @Override
    public List<AdminAuditEntry> findAll(int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "timestamp"));
        return jpaRepository.findAll(pageable).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AdminAuditEntry> findByAdminId(UUID adminId, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "timestamp"));
        return jpaRepository.findByActorAdminId(adminId, pageable).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AdminAuditEntry> findByEntidad(String entidad, String idEntidad, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "timestamp"));
        return jpaRepository.findByEntidadAfectadaAndIdEntidad(entidad, idEntidad, pageable).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }

    private AdminAuditEntry toDomain(AdminAuditLogJpaEntity e) {
        return AdminAuditEntry.builder()
                .id(e.getId())
                .actorAdminId(e.getActorAdminId())
                .accion(e.getAccion())
                .entidadAfectada(e.getEntidadAfectada())
                .idEntidad(e.getIdEntidad())
                .valoresAnteriores(e.getValoresAnteriores())
                .valoresNuevos(e.getValoresNuevos())
                .motivo(e.getMotivo())
                .ipOrigen(e.getIpOrigen())
                .timestamp(e.getTimestamp())
                .build();
    }
}
