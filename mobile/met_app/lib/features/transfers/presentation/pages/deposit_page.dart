import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../providers/deposit_provider.dart';

class DepositPage extends ConsumerStatefulWidget {
  final String method;
  const DepositPage({super.key, required this.method});

  @override
  ConsumerState<DepositPage> createState() => _DepositPageState();
}

class _DepositPageState extends ConsumerState<DepositPage> {
  final _amountController = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(depositProvider.notifier).reset();
      ref.read(depositProvider.notifier).setMethod(widget.method);
    });

    _amountController.addListener(() {
      final value = double.tryParse(_amountController.text) ?? 0.0;
      ref.read(depositProvider.notifier).setAmount(value);
    });
  }

  @override
  void dispose() {
    _amountController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(depositProvider);

    ref.listen(depositProvider, (previous, next) {
      if (next.error != null && next.error != previous?.error) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.error!),
            backgroundColor: Colors.redAccent,
          ),
        );
      } else if (next.isSuccess && !(previous?.isSuccess ?? false)) {
        _showSuccessDialog();
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: Text('Recargar con \${widget.method}'),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                '¿Cuánto deseas recargar?',
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 48),
              TextField(
                controller: _amountController,
                keyboardType: TextInputType.number,
                style: const TextStyle(
                  fontSize: 48,
                  fontWeight: FontWeight.bold,
                ),
                textAlign: TextAlign.center,
                decoration: InputDecoration(
                  prefixText: '\$ ',
                  border: InputBorder.none,
                  hintText: '0.00',
                  hintStyle: TextStyle(color: Colors.grey.withOpacity(0.5)),
                ),
              ).animate().fadeIn().slideY(begin: 0.2, end: 0),
              const Spacer(),
              ElevatedButton(
                onPressed: state.isLoading || state.amount <= 0
                    ? null
                    : () {
                        ref.read(depositProvider.notifier).submitDeposit();
                      },
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                ),
                child: state.isLoading
                    ? const SizedBox(
                        height: 24,
                        width: 24,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text(
                        'Confirmar Recarga',
                        style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                      ),
              ).animate().fadeIn(delay: 200.ms),
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
            const Icon(Icons.check_circle, color: Colors.green, size: 64)
                .animate()
                .scale(duration: 400.ms, curve: Curves.elasticOut),
            const SizedBox(height: 16),
            const Text(
              '¡Recarga Exitosa!',
              style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              'Tu saldo ha sido actualizado.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey[600]),
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () {
                  context.pop(); // Close dialog
                  context.go('/home'); // Go to home
                },
                child: const Text('Volver al Inicio'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
