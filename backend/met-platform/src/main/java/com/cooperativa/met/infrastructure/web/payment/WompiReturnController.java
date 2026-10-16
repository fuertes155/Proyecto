package com.cooperativa.met.infrastructure.web.payment;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Página a la que Wompi redirige al usuario al terminar el checkout o el pago PSE
 * (configurar {@code WOMPI_REDIRECT_URL=https://<host>/api/v1/payments/wompi/return}).
 *
 * <p>Solo informa: la redirección de Wompi NO confirma el pago. El saldo se acredita
 * únicamente con el webhook firmado ({@code /v1/webhooks/payment}), y la app detecta
 * el cambio desde su pantalla de espera.</p>
 *
 * <p>No refleja ningún parámetro de la URL (Wompi agrega {@code ?id=…}): el HTML es
 * estático, así que no hay superficie de XSS. Lleva su propia CSP porque la global
 * ({@code default-src 'self'}) bloquearía los estilos en línea; aquí no se permite JS.</p>
 */
@RestController
@RequestMapping("/v1/payments/wompi")
public class WompiReturnController {

    static final String CONTENT_SECURITY_POLICY =
            "default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

    private static final String PAGE = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>MET · Pago en proceso</title>
              <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                  background: #f4f6fb; color: #1b2340; min-height: 100vh;
                  display: flex; align-items: center; justify-content: center; padding: 24px 16px;
                }
                .card {
                  background: #fff; border-radius: 20px; padding: 32px 24px; width: 100%; max-width: 400px;
                  text-align: center; box-shadow: 0 12px 32px rgba(16, 32, 80, 0.12);
                }
                .icon {
                  width: 64px; height: 64px; border-radius: 50%; margin: 0 auto 20px; background: #e8eeff;
                  display: flex; align-items: center; justify-content: center; font-size: 30px;
                }
                h1 { font-size: 20px; margin-bottom: 12px; }
                p { font-size: 15px; line-height: 1.55; color: #4a5372; margin-bottom: 24px; }
                a.btn {
                  display: block; background: #0044ff; color: #fff; text-decoration: none; font-weight: 700;
                  padding: 15px 20px; border-radius: 14px; font-size: 16px;
                }
                small { display: block; margin-top: 16px; font-size: 12px; color: #8a91a8; }
                @media (prefers-color-scheme: dark) {
                  body { background: #0f1424; color: #e7ebf7; }
                  .card { background: #1a2136; box-shadow: none; }
                  .icon { background: #24305a; }
                  p { color: #b4bdd6; }
                  small { color: #7d86a3; }
                }
              </style>
            </head>
            <body>
              <main class="card">
                <div class="icon" aria-hidden="true">&#9203;</div>
                <h1>Estamos confirmando tu pago</h1>
                <p>Vuelve a la app MET. Tu saldo se actualizará automáticamente en cuanto el banco confirme la transacción.</p>
                <a class="btn" href="metapp://deposit/success">Volver a la app</a>
                <small>Ya puedes cerrar esta ventana.</small>
              </main>
            </body>
            </html>
            """;

    @GetMapping("/return")
    public ResponseEntity<String> paymentReturn() {
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, java.nio.charset.StandardCharsets.UTF_8))
                .header("Content-Security-Policy", CONTENT_SECURITY_POLICY)
                .body(PAGE);
    }
}
