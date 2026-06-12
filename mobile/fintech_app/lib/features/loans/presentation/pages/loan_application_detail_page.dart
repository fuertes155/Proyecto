import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/utils/currency_formatter.dart';
import '../providers/loan_provider.dart';

class LoanApplicationDetailPage extends ConsumerWidget {
  const LoanApplicationDetailPage({super.key, required this.applicationId});

  final String applicationId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final appAsync = ref.watch(loanApplicationDetailProvider(applicationId));

    return Scaffold(
      appBar: AppBar(title: const Text('Detalle solicitud')),
      body: appAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Error: $e')),
        data: (app) => SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(formatCop(app.amount), style: Theme.of(context).textTheme.headlineLarge),
              Text(app.statusLabel, style: const TextStyle(fontSize: 18)),
              const SizedBox(height: 8),
              Text('Propósito: ${app.purpose}'),
              const SizedBox(height: 24),
              _Row('Cuota mensual', formatCop(app.monthlyPayment)),
              _Row('Total a pagar', formatCop(app.totalPayment)),
              _Row('Plazo', '${app.termMonths} meses'),
              const SizedBox(height: 24),
              const Text('Plan de amortización',
                  style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600)),
              ...app.schedule.map(
                (i) => ListTile(
                  title: Text('Cuota ${i.installmentNumber}: ${formatCop(i.paymentAmount)}'),
                  subtitle: Text('Vence: ${i.dueDate} · Saldo: ${formatCop(i.remainingBalance)}'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Row extends StatelessWidget {
  const _Row(this.label, this.value);
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label),
          Text(value, style: const TextStyle(fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}
