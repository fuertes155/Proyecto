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
                (i) {
                  final isLate = i.status == 'LATE';
                  
                  return Card(
                    color: isLate ? Colors.red.shade50 : null,
                    shape: isLate ? RoundedRectangleBorder(
                      side: const BorderSide(color: Colors.red, width: 1.5),
                      borderRadius: BorderRadius.circular(12),
                    ) : null,
                    child: ListTile(
                      title: Text(
                        'Cuota ${i.installmentNumber}: ${formatCop(i.paymentAmount)}',
                        style: TextStyle(
                          fontWeight: isLate ? FontWeight.bold : FontWeight.normal,
                          color: isLate ? Colors.red.shade900 : null,
                        ),
                      ),
                      subtitle: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Vence: ${i.dueDate} · Saldo: ${formatCop(i.remainingBalance)}'),
                          if (isLate) ...[
                            const SizedBox(height: 4),
                            Text(
                              'Interés moratorio: ${formatCop(i.penaltyInterestAmount)} (Tasa de usura)',
                              style: const TextStyle(color: Colors.red, fontWeight: FontWeight.bold, fontSize: 12),
                            ),
                          ],
                        ],
                      ),
                      trailing: isLate 
                          ? const Icon(Icons.warning, color: Colors.red) 
                          : i.status == 'PAID'
                              ? const Icon(Icons.check_circle, color: Colors.green)
                              : null,
                    ),
                  );
                },
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
