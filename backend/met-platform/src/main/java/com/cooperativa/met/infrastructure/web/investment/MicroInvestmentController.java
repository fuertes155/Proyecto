package com.cooperativa.met.infrastructure.web.investment;

import com.cooperativa.met.application.investment.dto.CreatePortfolioRequest;
import com.cooperativa.met.application.investment.dto.PortfolioResponse;
import com.cooperativa.met.application.investment.usecase.*;
import com.cooperativa.met.domain.investment.model.InvestmentInstrument;
import com.cooperativa.met.domain.investment.model.InvestmentReturn;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API de micro-inversiones para el usuario final.
 *
 * GET  /v1/investments/instruments          → lista instrumentos disponibles
 * POST /v1/investments/portfolio            → crear portfolio (distribuir dinero)
 * GET  /v1/investments/portfolio            → listar mis portfolios
 * GET  /v1/investments/portfolio/{id}       → detalle de un portfolio
 * DELETE /v1/investments/portfolio/{id}     → cancelar portfolio (retira capital)
 * GET  /v1/investments/returns              → historial de rendimientos cobrados
 */
@RestController
@RequestMapping("/v1/investments")
@RequiredArgsConstructor
public class MicroInvestmentController {

    private final ListInvestmentInstrumentsUseCase listInstrumentsUseCase;
    private final CreateMicroInvestmentPortfolioUseCase createPortfolioUseCase;
    private final GetInvestmentPortfolioUseCase getPortfolioUseCase;
    private final CancelMicroInvestmentUseCase cancelUseCase;
    private final GetInvestmentReturnsUseCase getReturnsUseCase;

    /** Lista los instrumentos de inversión activos disponibles para el usuario. */
    @GetMapping("/instruments")
    public ResponseEntity<List<InvestmentInstrument>> listInstruments() {
        return ResponseEntity.ok(listInstrumentsUseCase.listActivos());
    }

    /** Crea un portfolio distribuyendo el dinero entre los instrumentos disponibles. */
    @PostMapping("/portfolio")
    public ResponseEntity<PortfolioResponse> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        PortfolioResponse response = createPortfolioUseCase.execute(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Lista todos los portfolios del usuario autenticado. */
    @GetMapping("/portfolio")
    public ResponseEntity<List<PortfolioResponse>> listPortfolios(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return ResponseEntity.ok(getPortfolioUseCase.listByUser(userId));
    }

    /** Obtiene el detalle de un portfolio específico. */
    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<PortfolioResponse> getPortfolio(
            @PathVariable UUID portfolioId) {
        return ResponseEntity.ok(getPortfolioUseCase.getById(portfolioId));
    }

    /** Cancela un portfolio antes del vencimiento. Solo devuelve el capital. */
    @DeleteMapping("/portfolio/{portfolioId}")
    public ResponseEntity<Map<String, String>> cancelPortfolio(
            @PathVariable UUID portfolioId,
            Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        cancelUseCase.execute(userId, portfolioId);
        return ResponseEntity.ok(Map.of(
                "message", "Portfolio cancelado. El capital ha sido devuelto a tu saldo."));
    }

    /** Historial de rendimientos cobrados por el usuario. */
    @GetMapping("/returns")
    public ResponseEntity<List<InvestmentReturn>> getReturns(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return ResponseEntity.ok(getReturnsUseCase.listByUser(userId));
    }
}
