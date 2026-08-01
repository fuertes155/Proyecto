import 'package:flutter/foundation.dart' show kDebugMode, kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../../core/network/api_client_provider.dart';
import '../../../../core/storage/secure_storage_service.dart';

/// Página de pago que se adapta a la plataforma:
/// - **Web (Chrome/Firefox):** Abre el checkout en nueva pestaña + muestra pantalla de espera.
/// - **Móvil (Android/iOS):** [Próximamente] WebView embebido. Por ahora también usa nueva pestaña.
///
/// NOTA: webview_flutter no soporta Flutter Web. El WebView embebido se activará
/// en la versión de producción para Android/iOS usando compilación condicional.
class PaymentWebViewPage extends ConsumerStatefulWidget {
  final String paymentUrl;
  final String method;
  final double amount;

  const PaymentWebViewPage({
    super.key,
    required this.paymentUrl,
    required this.method,
    required this.amount,
  });

  @override
  ConsumerState<PaymentWebViewPage> createState() => _PaymentWebViewPageState();
}

class _PaymentWebViewPageState extends ConsumerState<PaymentWebViewPage> {
  bool _launched = false;
  bool _isLaunching = false;
  bool _confirming = false;

  @override
  void initState() {
    super.initState();
    // Abrimos la URL automáticamente al entrar a la pantalla
    WidgetsBinding.instance.addPostFrameCallback((_) => _launchPayment());
  }

  Future<void> _launchPayment() async {
    if (_launched || _isLaunching) return;
    setState(() => _isLaunching = true);

    final uri = Uri.parse(widget.paymentUrl);
    try {
      final launched = await launchUrl(
        uri,
        // En web: abre en nueva pestaña. En móvil: abre en navegador externo.
        mode: kIsWeb ? LaunchMode.platformDefault : LaunchMode.externalApplication,
      );
      if (launched) {
        setState(() {
          _launched = true;
          _isLaunching = false;
        });
      } else {
        throw Exception('launchUrl retornó false');
      }
    } catch (e) {
      setState(() => _isLaunching = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('No se pudo abrir la pasarela de pago: $e'),
            backgroundColor: Colors.red.shade700,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    }
  }

  /// SOLO builds debug: simula la aprobación del pago llamando directamente al
  /// webhook simulado del backend (perfil "dev"), para poder probar el motor de
  /// distribución de capital sin depender de credenciales reales de Wompi. Nunca
  /// se compila en un build de release (kDebugMode es una const en tiempo de
  /// compilación, el compilador elimina esta rama por completo).
  Future<void> _simulateApprovalIfDebug() async {
    if (!kDebugMode) return;
    setState(() => _confirming = true);
    try {
      final userId = await ref.read(secureStorageProvider).readUserId();
      if (userId == null) return;

      final dio = ref.read(apiClientProvider);
      final txId = 'TEST-${DateTime.now().millisecondsSinceEpoch}';
      await dio.post('/v1/webhooks/mock-payment', data: {
        'event': 'transaction.updated',
        'data': {
          'transactionId': txId,
          'status': 'APPROVED',
          'amount': widget.amount,
          'userId': userId,
        },
      });
    } catch (_) {
      // Si falla, no bloquea el flujo: el usuario igual llega a la pantalla de
      // espera y puede confirmar el pago por la vía real o reintentar luego.
    } finally {
      if (mounted) setState(() => _confirming = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.close, color: Colors.black87),
          onPressed: () => context.pop(),
        ),
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              widget.method,
              style: const TextStyle(
                color: Colors.black87,
                fontWeight: FontWeight.bold,
                fontSize: 16,
              ),
            ),
            Row(
              children: [
                Icon(Icons.lock_outline, size: 12, color: Colors.green.shade600),
                const SizedBox(width: 4),
                Text(
                  'Pago seguro',
                  style: TextStyle(color: Colors.green.shade600, fontSize: 11),
                ),
              ],
            ),
          ],
        ),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              // Ícono animado
              Container(
                padding: const EdgeInsets.all(28),
                decoration: BoxDecoration(
                  color: const Color(0xFFF0F4FF),
                  shape: BoxShape.circle,
                  boxShadow: [
                    BoxShadow(
                      color: const Color(0xFF0044ff).withOpacity(0.15),
                      blurRadius: 24,
                      spreadRadius: 4,
                    ),
                  ],
                ),
                child: const Icon(Icons.open_in_new_rounded, size: 52, color: Color(0xFF0044ff)),
              ),
              const SizedBox(height: 32),

              const Text(
                'Completa tu pago',
                style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.black87),
              ),
              const SizedBox(height: 12),

              Text(
                _launched
                    ? 'Se abrió la pasarela de pago de ${widget.method}. Cuando termines, '
                      'regresa aquí y presiona "Ya pagué".'
                    : 'Preparando la pasarela de pago de ${widget.method}...',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 14, color: Colors.grey.shade600, height: 1.6),
              ),
              const SizedBox(height: 40),

              // Indicador de carga o estado
              if (_isLaunching)
                const CircularProgressIndicator(color: Color(0xFF0044ff), strokeWidth: 2)
              else if (_launched)
                Column(
                  children: [
                    const CircularProgressIndicator(
                      color: Color(0xFF0044ff),
                      strokeWidth: 2,
                    ),
                    const SizedBox(height: 12),
                    Text(
                      'Esperando confirmación de pago...',
                      style: TextStyle(fontSize: 13, color: Colors.grey.shade500),
                    ),
                  ],
                ),

              const SizedBox(height: 40),

              // Botón principal: "Ya pagué"
              if (_launched)
                SizedBox(
                  width: double.infinity,
                  height: 54,
                  child: ElevatedButton(
                    onPressed: _confirming
                        ? null
                        : () async {
                            await _simulateApprovalIfDebug();
                            if (!mounted) return;
                            context.pushReplacement('/deposit/waiting', extra: {
                              'method': widget.method,
                              'amount': widget.amount,
                            });
                          },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF0044ff),
                      foregroundColor: Colors.white,
                      elevation: 0,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                    ),
                    child: _confirming
                        ? const SizedBox(
                            width: 22,
                            height: 22,
                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                          )
                        : const Text(
                            'Ya pagué, continuar',
                            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                          ),
                  ),
                ),

              const SizedBox(height: 12),

              // Botón secundario: Reabrir
              OutlinedButton.icon(
                onPressed: _isLaunching ? null : () {
                  _launched = false;
                  _launchPayment();
                },
                icon: const Icon(Icons.refresh, size: 18),
                label: const Text('Abrir pasarela de nuevo'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: const Color(0xFF0044ff),
                  side: const BorderSide(color: Color(0xFF0044ff)),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
