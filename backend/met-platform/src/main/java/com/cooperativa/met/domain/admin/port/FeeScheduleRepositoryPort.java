package com.cooperativa.met.domain.admin.port;

import com.cooperativa.met.domain.admin.model.FeeSchedule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeeScheduleRepositoryPort {
    List<FeeSchedule> findAll();
    List<FeeSchedule> findVigentes();
    Optional<FeeSchedule> findById(UUID id);
    FeeSchedule save(FeeSchedule fee);
    /** Cierra la vigencia de la tarifa anterior del mismo tipo */
    void cerrarVigencia(String tipoTarifa);
    /** Elimina permanentemente una tarifa por id */
    void deleteById(UUID id);
}
