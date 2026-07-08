import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../providers/admin_business_provider.dart';

class LoansPage extends ConsumerWidget {
  const LoansPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final loansAsync = ref.watch(loansProvider);
    final primary = Theme.of(context).colorScheme.primary;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Solicitudes de Crédito', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
      ),
      body: loansAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(child: Text('Error: $err')),
        data: (loans) {
          if (loans.isEmpty) {
            return const Center(child: Text('No hay solicitudes de crédito.'));
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: loans.length,
            itemBuilder: (context, index) {
              final loan = loans[index];
              return Card(
                elevation: 0,
                color: Theme.of(context).colorScheme.surface.withOpacity(0.5),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                  side: BorderSide(color: primary.withOpacity(0.2)),
                ),
                margin: const EdgeInsets.only(bottom: 12),
                child: ListTile(
                  leading: CircleAvatar(
                    backgroundColor: primary.withOpacity(0.2),
                    child: Icon(Icons.request_quote, color: primary),
                  ),
                  title: Text('Préstamo ID: ${loan.id}', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                  subtitle: Text('Socio ID: ${loan.userId}'),
                  trailing: Chip(
                    label: Text(loan.status, style: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold)),
                    backgroundColor: loan.status == 'APPROVED' ? Colors.green.withOpacity(0.2) : Colors.orange.withOpacity(0.2),
                    side: BorderSide.none,
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
