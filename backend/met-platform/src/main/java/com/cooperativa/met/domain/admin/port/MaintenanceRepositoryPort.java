package com.cooperativa.met.domain.admin.port;

import com.cooperativa.met.domain.admin.model.MaintenanceWindow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaintenanceRepositoryPort {
    List<MaintenanceWindow> findAll();
    Optional<MaintenanceWindow> findById(UUID id);
    Optional<MaintenanceWindow> findActiva();
    MaintenanceWindow save(MaintenanceWindow window);
    void deleteById(UUID id);
}
