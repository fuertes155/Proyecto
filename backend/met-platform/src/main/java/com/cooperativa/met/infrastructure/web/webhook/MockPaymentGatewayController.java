package com.cooperativa.met.infrastructure.web.webhook;

import com.cooperativa.met.application.account.dto.DepositRequest;
import com.cooperativa.met.application.account.usecase.ProcessWebhookUseCase;
import com.cooperativa.met.infrastructure.persistence.webhook.entity.ProcessedWebhookJpaEntity;
import com.cooperativa.met.infrastructure.persistence.webhook.repository.ProcessedWebhookJpaRepository;
import com.cooperativa.met.infrastructure.web.webhook.dto.PaymentWebhookPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/v1")
@Profile("dev | demo")
@RequiredArgsConstructor
@Slf4j
public class MockPaymentGatewayController {

    private final ProcessWebhookUseCase processWebhookUseCase;
    private final ProcessedWebhookJpaRepository processedWebhookRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/webhooks/mock-payment")
    public ResponseEntity<Void> handleMockPaymentWebhook(@RequestBody String rawPayload) {
        try {
            log.info("[DEV] Received mock payment webhook. Auto-signing payload.");
            PaymentWebhookPayload payload = objectMapper.readValue(rawPayload, PaymentWebhookPayload.class);
            
            if (!"transaction.updated".equals(payload.getEvent()) || payload.getData() == null) {
                return ResponseEntity.ok().build();
            }

            var data = payload.getData();
            if (!"APPROVED".equals(data.getStatus()) || data.getUserId() == null) {
                return ResponseEntity.ok().build();
            }

            String txId = data.getTransactionId();
            if (processedWebhookRepository.existsById(txId)) {
                log.info("Mock transaction {} already processed. Ignoring.", txId);
                return ResponseEntity.ok().build();
            }

            String method = "PSE_MOCK_" + txId;
            processWebhookUseCase.execute(txId, "MOCK_GATEWAY", data.getUserId(), data.getAmount(), method);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[DEV] Error processing mock payment webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/mock-payment-gateway", produces = "text/html")
    public String renderGateway(
            @RequestParam("transactionId") String transactionId,
            @RequestParam("amount") String amount,
            @RequestParam("userId") String userId) {

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Pasarela PSE Simulada</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background: linear-gradient(135deg, #1a237e 0%%, #0d47a1 100%%);
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                    }
                    .card {
                        background: white;
                        border-radius: 16px;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                        padding: 40px 32px;
                        width: 100%%;
                        max-width: 420px;
                        text-align: center;
                    }
                    .logo { font-size: 12px; color: #888; margin-bottom: 12px; letter-spacing: 2px; text-transform: uppercase; }
                    h1 { color: #1a237e; font-size: 22px; margin-bottom: 8px; }
                    .subtitle { color: #666; font-size: 14px; margin-bottom: 28px; }
                    .amount-box {
                        background: #f0f4ff;
                        border-radius: 12px;
                        padding: 20px;
                        margin-bottom: 12px;
                    }
                    .amount-label { font-size: 12px; color: #888; margin-bottom: 4px; }
                    .amount { font-size: 36px; font-weight: 800; color: #1a237e; }
                    .info { font-size: 11px; color: #bbb; margin-bottom: 28px; word-break: break-all; }
                    .btn {
                        background: linear-gradient(135deg, #0044ff, #0033cc);
                        color: white;
                        border: none;
                        border-radius: 10px;
                        padding: 16px 24px;
                        font-size: 16px;
                        font-weight: bold;
                        width: 100%%;
                        cursor: pointer;
                        transition: all 0.2s;
                        letter-spacing: 0.5px;
                    }
                    .btn:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(0,68,255,0.4); }
                    .btn:disabled { background: #ccc; cursor: not-allowed; transform: none; box-shadow: none; }
                    #status { margin-top: 16px; font-size: 14px; color: #666; min-height: 20px; }
                    .success { color: #2e7d32; font-weight: bold; }
                    .error { color: #c62828; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">🏦 Entidad Bancaria PSE</div>
                    <h1>Pasarela de Pago</h1>
                    <p class="subtitle">Autorización de débito PSE simulado</p>
                    <div class="amount-box">
                        <div class="amount-label">Monto a pagar</div>
                        <div class="amount">$ %s</div>
                    </div>
                    <p class="info">Transacción: %s</p>
                    <button class="btn" id="payButton" onclick="processPayment()">✓ Confirmar Pago</button>
                    <div id="status"></div>
                </div>

                <script>
                    function processPayment() {
                        const btn = document.getElementById('payButton');
                        const status = document.getElementById('status');
                        
                        btn.disabled = true;
                        btn.innerText = 'Procesando...';
                        status.innerText = 'Simulando autorización bancaria...';

                        const payload = {
                            event: "transaction.updated",
                            data: {
                                transactionId: "%s",
                                status: "APPROVED",
                                amount: %s,
                                userId: "%s"
                            }
                        };

                        fetch('/api/v1/webhooks/mock-payment', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(payload)
                        })
                        .then(response => {
                            if (response.ok) {
                                btn.innerText = '✓ Pago Aprobado';
                                btn.style.background = 'linear-gradient(135deg, #2e7d32, #1b5e20)';
                                status.innerHTML = '<span class="success">✓ Pago confirmado exitosamente. Ya puedes cerrar esta ventana.</span>';
                            } else {
                                status.innerHTML = '<span class="error">✗ Error procesando el pago. Intenta de nuevo.</span>';
                                btn.disabled = false;
                                btn.innerText = 'Reintentar';
                            }
                        })
                        .catch(err => {
                            console.error(err);
                            status.innerHTML = '<span class="error">✗ Error de red. Intenta de nuevo.</span>';
                            btn.disabled = false;
                            btn.innerText = 'Reintentar';
                        });
                    }
                </script>
            </body>
            </html>
            """.formatted(amount, transactionId, transactionId, amount, userId);
    }
}
