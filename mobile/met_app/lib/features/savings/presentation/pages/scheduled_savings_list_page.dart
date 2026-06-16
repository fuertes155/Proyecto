import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/utils/currency_formatter.dart';
import '../../../../core/widgets/accessible_button.dart';
import '../../data/models/scheduled_savings_models.dart';
import '../providers/scheduled_savings_provider.dart';

class ScheduledSavingsListPage extends ConsumerStatefulWidget {
  const ScheduledSavingsListPage({super.key});

  @override
  ConsumerState<ScheduledSavingsListPage> createState() => _ScheduledSavingsListPageState();
}

class _ScheduledSavingsListPageState extends ConsumerState<ScheduledSavingsListPage> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(scheduledSavingsListProvider.notifier).load());
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(scheduledSavingsListProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Ahorro programado')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/savings/scheduled/create'),
        icon: const Icon(Icons.add),
        label: const Text('Nueva meta'),
      ),
      body: state.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('Error: $error'),
              const SizedBox(height: 16),
              AccessibleButton(
                label: 'Reintentar',
                onPressed: () => ref.read(scheduledSavingsListProvider.notifier).load(),
              ),
            ],
          ),
        ),
        data: (accounts) {
          if (accounts.isEmpty) {
            return Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.savings_outlined, size: 72),
                  const SizedBox(height: 16),
                  const Text(
                    'Aún no tienes metas de ahorro',
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Crea tu primera meta con aportes automáticos semanales, quincenales o mensuales.',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 16),
                  ),
                  const SizedBox(height: 24),
                  AccessibleButton(
                    label: 'Crear meta de ahorro',
                    onPressed: () => context.push('/savings/scheduled/create'),
                  ),
                ],
              ),
            );
          }

          return RefreshIndicator(
            onRefresh: () => ref.read(scheduledSavingsListProvider.notifier).load(),
            child: ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: accounts.length,
              separatorBuilder: (_, __) => const SizedBox(height: 12),
              itemBuilder: (context, index) => _AccountCard(account: accounts[index]),
            ),
          );
        },
      ),
    );
  }
}

class _AccountCard extends StatelessWidget {
  const _AccountCard({required this.account});

  final ScheduledSavingsAccount account;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () => context.push('/savings/scheduled/${account.id}'),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      account.name,
                      style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
                    ),
                  ),
                  _StatusChip(status: account.status, label: account.statusLabel),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                formatCop(account.currentBalance),
                style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
              ),
              if (account.targetAmount != null) ...[
                const SizedBox(height: 8),
                LinearProgressIndicator(value: account.progressPercentage / 100),
                const SizedBox(height: 4),
                Text(
                  '${account.progressPercentage.toStringAsFixed(0)}% de ${formatCop(account.targetAmount!)}',
                  style: const TextStyle(fontSize: 14),
                ),
              ],
              const SizedBox(height: 12),
              Text(
                'Aporte ${account.frequencyLabel.toLowerCase()}: ${formatCop(account.contributionAmount)}',
                style: const TextStyle(fontSize: 16),
              ),
              Text(
                'Próximo aporte: ${account.nextContributionDate}',
                style: TextStyle(fontSize: 14, color: Colors.grey.shade700),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.status, required this.label});

  final String status;
  final String label;

  @override
  Widget build(BuildContext context) {
    final color = switch (status) {
      'ACTIVE' => Colors.green,
      'PAUSED' => Colors.orange,
      'COMPLETED' => Colors.blue,
      _ => Colors.grey,
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(label, style: TextStyle(color: color, fontWeight: FontWeight.w600)),
    );
  }
}
