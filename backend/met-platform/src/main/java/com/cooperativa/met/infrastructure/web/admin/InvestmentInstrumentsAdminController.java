package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.investment.usecase.ListInvestmentInstrumentsUseCase;
import com.cooperativa.met.domain.investment.model.InvestmentInstrument;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.cooperativa.met.domain.investment.port.InvestmentInstrumentPort;

/**
 * API admin para gestionar instrumentos de inversión.
 *
 * GET    /v1/admin/investment-instruments          → listar todos
 * POST   /v1/admin/investment-instruments          → crear nuevo
 * PATCH  /v1/admin/investment-instruments/{id}/toggle → activar/desactivar
 * DELETE /v1/admin/investment-instruments/{id}     → eliminar
 */
@RestController
@RequestMapping("/v1/admin/investment-instruments")
@RequiredArgsConstructor
public class InvestmentInstrumentsAdminController {

    private final InvestmentInstrumentPort instrumentPort;
    private final ListInvestmentInstrumentsUseCase listUseCase;

    @GetMapping
    public ResponseEntity<List<InvestmentInstrument>> getAll() {
        return ResponseEntity.ok(listUseCase.listAll());
    }

    @PostMapping
    public ResponseEntity<InvestmentInstrument> create(
            @Valid @RequestBody CreateInstrumentRequest request,
            Authentication auth) {
        UUID adminId = (UUID) auth.getPrincipal();
        InvestmentInstrument instrument = InvestmentInstrument.builder()
                .id(UUID.randomUUID())
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .tasaAnual(request.tasaAnual())
                .plazoDias(request.plazoDias())
                .montoMinimo(request.montoMinimo())
                .cupoMaximo(request.cupoMaximo())
                .activo(true)
                .creadoPor(adminId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(instrumentPort.save(instrument));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<InvestmentInstrument> toggle(
            @PathVariable UUID id,
            @RequestParam boolean activo) {
        InvestmentInstrument instrument = instrumentPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instrumento no encontrado: " + id));
        return ResponseEntity.ok(instrumentPort.save(instrument.withActivo(activo)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        instrumentPort.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Instrumento de inversión eliminado"));
    }

    /** DTO interno para crear instrumentos desde el admin. */
    public record CreateInstrumentRequest(
            @NotBlank(message = "El nombre es requerido")
            String nombre,

            String descripcion,

            @NotNull(message = "La tasa anual es requerida")
            @DecimalMin(value = "0.001", message = "La tasa debe ser mayor a 0")
            BigDecimal tasaAnual,

            @Min(value = 1, message = "El plazo mínimo es 1 día")
            int plazoDias,

            @NotNull(message = "El monto mínimo es requerido")
            @DecimalMin(value = "1000", message = "El monto mínimo debe ser al menos $1.000")
            BigDecimal montoMinimo,

            BigDecimal cupoMaximo
    ) {}
}
