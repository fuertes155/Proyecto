import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/utils/currency_formatter.dart';
import '../providers/loan_provider.dart';

class LoanApplicationsListPage extends ConsumerWidget {
  const LoanApplicationsListPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final applicationsAsync = ref.watch(loanApplicationsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Mis solicitudes'),
        actions: [
          IconButton(
            icon: const Icon(Icons.calculate),
            onPressed: () => context.push('/loans/simulate'),
          ),
        ],
      ),
      body: applicationsAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Error: $e')),
        data: (apps) {
          if (apps.isEmpty) {
            return const Center(child: Text('No tienes solicitudes de préstamo'));
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: apps.length,
            itemBuilder: (_, i) {
              final app = apps[i];
              return Card(
                child: ListTile(
                  title: Text(formatCop(app.amount)),
                  subtitle: Text('${app.termMonths} meses · ${app.statusLabel}'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/loans/applications/${app.id}'),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
