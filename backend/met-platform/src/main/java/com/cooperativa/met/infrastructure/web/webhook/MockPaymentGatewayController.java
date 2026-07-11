package com.cooperativa.met.infrastructure.web.webhook;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class MockPaymentGatewayController {

    @GetMapping(value = "/mock-payment-gateway", produces = "text/html")
    public String renderGateway(
            @RequestParam("transactionId") String transactionId,
            @RequestParam("amount") String amount,
            @RequestParam("userId") String userId,
            @RequestParam(value = "returnUrl", required = false, defaultValue = "") String returnUrl) {

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Mock Wompi / PSE Gateway</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        background-color: #f4f6f8;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        height: 100vh;
                        margin: 0;
                    }
                    .card {
                        background: white;
                        border-radius: 12px;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                        padding: 32px;
                        width: 100%;
                        max-width: 400px;
                        text-align: center;
                    }
                    h1 {
                        color: #1a237e;
                        font-size: 24px;
                        margin-bottom: 24px;
                    }
                    .amount {
                        font-size: 32px;
                        font-weight: bold;
                        color: #333;
                        margin-bottom: 32px;
                    }
                    .btn {
                        background-color: #0044ff;
                        color: white;
                        border: none;
                        border-radius: 6px;
                        padding: 16px 24px;
                        font-size: 16px;
                        font-weight: bold;
                        width: 100%;
                        cursor: pointer;
                        transition: background-color 0.2s;
                    }
                    .btn:hover {
                        background-color: #0033cc;
                    }
                    .btn:disabled {
                        background-color: #cccccc;
                        cursor: not-allowed;
                    }
                    #status {
                        margin-top: 16px;
                        color: #666;
                        font-size: 14px;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>Pasarela de Pago Simulada</h1>
                    <p>Estás a punto de pagar con PSE</p>
                    <div class="amount">$ %s</div>
                    <button class="btn" id="payButton" onclick="processPayment()">Confirmar Pago (MOCK)</button>
                    <div id="status"></div>
                </div>

                <script>
                    function processPayment() {
                        const btn = document.getElementById('payButton');
                        const status = document.getElementById('status');
                        
                        btn.disabled = true;
                        btn.innerText = "Procesando...";
                        status.innerText = "Simulando autorización con tu banco...";

                        const payload = {
                            event: "transaction.updated",
                            data: {
                                transactionId: "%s",
                                status: "APPROVED",
                                amount: %s,
                                userId: "%s"
                            }
                        };

                        // Send Webhook to Backend
                        fetch('/v1/webhooks/payment', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify(payload)
                        })
                        .then(response => {
                            if (response.ok) {
                                status.innerText = "Pago Aprobado. Redirigiendo...";
                                status.style.color = "green";
                                setTimeout(() => {
                                    const returnUrl = "%s";
                                    if (returnUrl) {
                                        window.location.href = returnUrl;
                                    } else {
                                        status.innerText = "Pago completado. Puedes cerrar esta ventana.";
                                    }
                                }, 1500);
                            } else {
                                status.innerText = "Error procesando webhook.";
                                status.style.color = "red";
                                btn.disabled = false;
                                btn.innerText = "Intentar de nuevo";
                            }
                        })
                        .catch(err => {
                            console.error(err);
                            status.innerText = "Error de red.";
                            status.style.color = "red";
                            btn.disabled = false;
                            btn.innerText = "Intentar de nuevo";
                        });
                    }
                </script>
            </body>
            </html>
            """.formatted(amount, transactionId, amount, userId, returnUrl);
    }
}
