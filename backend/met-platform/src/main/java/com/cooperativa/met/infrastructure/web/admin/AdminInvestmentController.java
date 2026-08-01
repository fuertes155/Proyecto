package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.investment.dto.GuaranteeFundSummaryResponse;
import com.cooperativa.met.application.investment.dto.InvestmentBreakdownItemResponse;
import com.cooperativa.met.application.investment.usecase.GetGuaranteeFundSummaryUseCase;
import com.cooperativa.met.application.investment.usecase.GetInvestorFundingBreakdownUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * A qué deudores concretos quedó fraccionado y emparejado el capital de un
 * inversionista. Es información sensible de otro socio (su nombre asociado
 * al préstamo), así que solo un administrador puede consultarla — el propio
 * inversionista solo ve el resumen agregado en GET /v1/investments/my-summary.
 */
@RestController
@RequestMapping("/v1/admin/investments")
@RequiredArgsConstructor
public class AdminInvestmentController {

    private final GetInvestorFundingBreakdownUseCase getFundingBreakdownUseCase;
    private final GetGuaranteeFundSummaryUseCase getGuaranteeFundSummaryUseCase;

    @GetMapping("/investors/{userId}/breakdown")
    public ResponseEntity<List<InvestmentBreakdownItemResponse>> getInvestorBreakdown(@PathVariable UUID userId) {
        return ResponseEntity.ok(getFundingBreakdownUseCase.execute(userId));
    }

    /** Saldo y movimientos del Fondo de Garantías/Aval Colectivo. */
    @GetMapping("/guarantee-fund")
    public ResponseEntity<GuaranteeFundSummaryResponse> getGuaranteeFund() {
        return ResponseEntity.ok(getGuaranteeFundSummaryUseCase.execute());
    }
}
