import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'dart:async';
import '../providers/deposit_provider.dart';
import '../providers/transfers_provider.dart';

class DepositWaitingPage extends ConsumerStatefulWidget {
  final String method;
  final double amount;
  const DepositWaitingPage({super.key, required this.method, required this.amount}) : _method = method, _amount = amount;
  final String _method;
  final double _amount;

  @override
  ConsumerState<DepositWaitingPage> createState() => _DepositWaitingPageState();
}

class _DepositWaitingPageState extends ConsumerState<DepositWaitingPage> {
  Timer? _countdownTimer;
  Timer? _pollTimer;
  int _secondsRemaining = 300; // 5 minutes
  bool _timedOut = false;

  @override
  void initState() {
    super.initState();
    _startCountdown();
    _startPolling();
  }

  void _startCountdown() {
    _countdownTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_secondsRemaining > 0) {
        setState(() {
          _secondsRemaining--;
        });
      } else {
        timer.cancel();
        _pollTimer?.cancel();
        if (mounted && !ref.read(depositProvider).isSuccess) {
          setState(() => _timedOut = true);
        }
      }
    });
  }

  /// El pago real se confirma vía el webhook de Wompi en el backend, que
  /// acredita el saldo de la cuenta. Aquí solo observamos el saldo del
  /// usuario y detectamos ese acredite — no hay ningún atajo client-side.
  void _startPolling() {
    _pollTimer = Timer.periodic(const Duration(seconds: 3), (timer) async {
      if (!mounted) return;
      final baseline = ref.read(depositProvider).baselineBalance;
      if (baseline == null) return; // aún no se cargó el saldo previo al pago

      ref.invalidate(myAccountProvider);
      final account = await ref.read(myAccountProvider.future);

      if (account.principalBalance >= baseline + widget._amount - 0.01) {
        timer.cancel();
        if (mounted) {
          ref.read(depositProvider.notifier).markSuccess();
        }
      }
    });
  }

  @override
  void dispose() {
    _countdownTimer?.cancel();
    _pollTimer?.cancel();
    super.dispose();
  }

  String get _formattedTime {
    final minutes = _secondsRemaining ~/ 60;
    final seconds = _secondsRemaining % 60;
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    final currencyFormat = NumberFormat.currency(locale: 'es_CO', symbol: 'COP ', decimalDigits: 2);
    final formattedAmount = currencyFormat.format(widget._amount);
    
    final state = ref.watch(depositProvider);

    ref.listen(depositProvider, (previous, next) {
      if (next.isSuccess && !(previous?.isSuccess ?? false)) {
        _showSuccessDialog();
      } else if (next.error != null) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(next.error!)));
        context.pop();
      }
    });

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        automaticallyImplyLeading: false, // Hide back button for waiting screen
        title: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              'MET',
              style: TextStyle(
                color: Color(0xFFE55B13), // Metano Orange
                fontWeight: FontWeight.w900,
                fontSize: 24,
                fontStyle: FontStyle.italic,
              ),
            ),
            Text(
              formattedAmount,
              style: const TextStyle(
                color: Colors.black87,
                fontSize: 16,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Divider(color: Colors.black12, thickness: 1),
              const SizedBox(height: 24),
              Text(
                'Finaliza el pago en tu ${widget._method}',
                style: const TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w900,
                  color: Color(0xFF001435), // Dark blue
                ),
              ),
              const SizedBox(height: 12),
              const Text(
                'Enviamos una notificación al +573000000000 para que autorices la compra.',
                style: TextStyle(
                  fontSize: 14,
                  color: Colors.black54,
                  height: 1.5,
                ),
              ),
              const SizedBox(height: 32),
              
              // Pay Card
              Container(
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  color: const Color(0xFFF3F2F1), // Very light grey
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        widget._method,
                        style: const TextStyle(
                          fontWeight: FontWeight.w900,
                          color: Color(0xFF28004D),
                          fontSize: 16,
                        ),
                      ),
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        const Text(
                          'Total a pagar',
                          style: TextStyle(
                            color: Colors.black54,
                            fontSize: 12,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          formattedAmount,
                          style: const TextStyle(
                            fontWeight: FontWeight.w900,
                            color: Color(0xFF001435),
                            fontSize: 18,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              
              const SizedBox(height: 24),
              Center(
                child: Text(
                  'Si no ves la notificación, búscala en tu app ${widget._method}',
                  style: const TextStyle(
                    color: Colors.black45,
                    fontSize: 13,
                  ),
                ),
              ),
              
              const Spacer(),
              
              // Timer
              Center(
                child: Column(
                  children: [
                    const Text(
                      'Tiempo restante para realizar la transferencia',
                      style: TextStyle(
                        color: Colors.black45,
                        fontSize: 12,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _formattedTime,
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 18,
                        color: Colors.black87,
                      ),
                    ),
                    if (_timedOut) ...[
                      const SizedBox(height: 16),
                      Text(
                        'No hemos recibido la confirmación todavía. Si ya pagaste, '
                        'puede tardar unos minutos más — revisa tu saldo en el inicio.',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.orange.shade800, fontSize: 13),
                      ),
                      const SizedBox(height: 12),
                      OutlinedButton(
                        onPressed: () => context.go('/home'),
                        child: const Text('Volver al inicio'),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }

  void _showSuccessDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.check_circle, color: Colors.green, size: 64),
            const SizedBox(height: 16),
            const Text(
              '¡Pago Exitoso!',
              style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              'El depósito ha sido confirmado.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey[600]),
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () {
                  context.pop();
                  context.go('/home');
                },
                style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFFE55B13), foregroundColor: Colors.white),
                child: const Text('Aceptar'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
