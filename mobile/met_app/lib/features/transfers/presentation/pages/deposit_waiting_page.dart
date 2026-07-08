import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'dart:async';
import '../providers/deposit_provider.dart';

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
  Timer? _timer;
  int _secondsRemaining = 300; // 5 minutes

  @override
  void initState() {
    super.initState();
    // Simulate webhook arrival after some seconds if keys are not provided yet
    _startTimer();
    _simulateWebhookArrival();
  }

  void _startTimer() {
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_secondsRemaining > 0) {
        setState(() {
          _secondsRemaining--;
        });
      } else {
        timer.cancel();
      }
    });
  }

  void _simulateWebhookArrival() async {
    // This is temporary until real Wompi webhook is implemented
    await Future.delayed(const Duration(seconds: 5));
    if (mounted) {
      ref.read(depositProvider.notifier).submitDeposit('3000000000'); // Send dummy phone for now
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
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
