import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../providers/home_provider.dart';
import '../../../../core/widgets/accessible_button.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final homeState = ref.watch(homeProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Resumen Financiero'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => ref.read(homeProvider.notifier).refresh(),
          ),
        ],
      ),
      body: homeState.when(
        data: (summary) {
          final currency = NumberFormat.currency(locale: 'es_CO', symbol: '\$', decimalDigits: 0);

          return RefreshIndicator(
            onRefresh: () => ref.read(homeProvider.notifier).refresh(),
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                // Resumen de la cuenta principal
                Card(
                  elevation: 4,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Cuenta Principal', style: Theme.of(context).textTheme.titleMedium),
                        const SizedBox(height: 8),
                        Text(
                          currency.format(summary.totalBalance),
                          style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 4),
                        Text('Capital: ${currency.format(summary.principalBalance)}', style: Theme.of(context).textTheme.bodyMedium),
                        Text('Intereses: ${currency.format(summary.interestBalance)}', style: Theme.of(context).textTheme.bodyMedium),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 16),

                // Resumen de otros productos
                Row(
                  children: [
                    Expanded(
                      child: _SummaryCard(
                        title: 'Ahorros',
                        amount: currency.format(summary.savingsTotal),
                        icon: Icons.savings,
                        color: Colors.green,
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: _SummaryCard(
                        title: 'Inversiones',
                        amount: currency.format(summary.investmentsTotal),
                        icon: Icons.trending_up,
                        color: Colors.blue,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 24),

                // Transacciones recientes
                Text('Transacciones Recientes', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 8),
                if (summary.recentTransactions.isEmpty)
                  const Padding(
                    padding: EdgeInsets.all(16.0),
                    child: Center(child: Text('No hay transacciones recientes')),
                  )
                else
                  ...summary.recentTransactions.map((tx) => ListTile(
                        leading: CircleAvatar(
                          backgroundColor: tx.isCredit ? Colors.green.withOpacity(0.1) : Colors.red.withOpacity(0.1),
                          child: Icon(
                            tx.isCredit ? Icons.arrow_downward : Icons.arrow_upward,
                            color: tx.isCredit ? Colors.green : Colors.red,
                          ),
                        ),
                        title: Text(tx.description),
                        subtitle: Text(DateFormat('dd MMM yyyy, HH:mm').format(tx.createdAt)),
                        trailing: Text(
                          '${tx.isCredit ? '+' : '-'}${currency.format(tx.amount)}',
                          style: TextStyle(
                            color: tx.isCredit ? Colors.green : Colors.red,
                            fontWeight: FontWeight.bold,
                            fontSize: 16,
                          ),
                        ),
                      )),
              ],
            ),
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, stack) => Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.error_outline, size: 48, color: Colors.red),
              const SizedBox(height: 16),
              Text('Error al cargar el resumen', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 8),
              AccessibleButton(
                label: 'Reintentar',
                onPressed: () => ref.read(homeProvider.notifier).refresh(),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SummaryCard extends StatelessWidget {
  final String title;
  final String amount;
  final IconData icon;
  final Color color;

  const _SummaryCard({
    required this.title,
    required this.amount,
    required this.icon,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: color),
            const SizedBox(height: 12),
            Text(title, style: Theme.of(context).textTheme.bodyMedium),
            const SizedBox(height: 4),
            Text(
              amount,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
            ),
          ],
        ),
      ),
    );
  }
}
