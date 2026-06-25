package com.cooperativa.met.application.admin.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record MaintenanceWindowRequest(
        @NotBlank(message = "La descripción es requerida")
        String descripcion,

        @NotNull(message = "El inicio es requerido")
        Instant inicio,

        @NotNull(message = "El fin es requerido")
        @Future(message = "El fin debe ser una fecha futura")
        Instant fin,

        /** Lista de rutas API que permanecen activas durante el mantenimiento */
        List<String> endpointsActivos
) {}
