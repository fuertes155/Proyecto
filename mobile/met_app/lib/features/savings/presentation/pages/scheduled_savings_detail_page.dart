import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/utils/currency_formatter.dart';
import '../../../../core/widgets/accessible_button.dart';
import '../providers/scheduled_savings_provider.dart';

class ScheduledSavingsDetailPage extends ConsumerWidget {
  const ScheduledSavingsDetailPage({super.key, required this.accountId});

  final String accountId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final accountAsync = ref.watch(scheduledSavingsDetailProvider(accountId));
    final contributionsAsync = ref.watch(contributionHistoryProvider(accountId));

    return Scaffold(
      appBar: AppBar(title: const Text('Detalle de meta')),
      body: accountAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(child: Text('Error: $error')),
        data: (account) => SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(account.name, style: Theme.of(context).textTheme.headlineLarge),
              const SizedBox(height: 8),
              Text(account.statusLabel, style: const TextStyle(fontSize: 16)),
              const SizedBox(height: 24),
              Text(
                formatCop(account.currentBalance),
                style: const TextStyle(fontSize: 32, fontWeight: FontWeight.bold),
              ),
              if (account.targetAmount != null) ...[
                const SizedBox(height: 12),
                LinearProgressIndicator(value: account.progressPercentage / 100),
                Text('${account.progressPercentage.toStringAsFixed(0)}% de la meta'),
              ],
              const SizedBox(height: 24),
              _InfoRow(label: 'Aporte', value: formatCop(account.contributionAmount)),
              _InfoRow(label: 'Frecuencia', value: account.frequencyLabel),
              _InfoRow(label: 'Próximo aporte', value: account.nextContributionDate),
              const SizedBox(height: 24),
              if (account.status == 'ACTIVE')
                AccessibleButton(
                  label: 'Pausar aportes',
                  onPressed: () async {
                    await ref.read(scheduledSavingsListProvider.notifier).pauseAccount(accountId);
                    ref.invalidate(scheduledSavingsDetailProvider(accountId));
                  },
                ),
              if (account.status == 'PAUSED')
                AccessibleButton(
                  label: 'Reactivar aportes',
                  onPressed: () async {
                    await ref.read(scheduledSavingsListProvider.notifier).resumeAccount(accountId);
                    ref.invalidate(scheduledSavingsDetailProvider(accountId));
                  },
                ),
              if (account.status == 'ACTIVE' || account.status == 'PAUSED') ...[
                const SizedBox(height: 12),
                OutlinedButton(
                  onPressed: () => _showPartialWithdrawalModal(context, ref, account),
                  child: const Text('Retiro Parcial (Max 40%)'),
                ),
                const SizedBox(height: 12),
                OutlinedButton(
                  style: OutlinedButton.styleFrom(foregroundColor: Colors.red),
                  onPressed: () => _showFullWithdrawalConfirm(context, ref, account),
                  child: const Text('Retirar todo y Cerrar cuenta'),
                ),
              ],
              const SizedBox(height: 32),
              const Text('Historial de aportes', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600)),
              const SizedBox(height: 12),
              contributionsAsync.when(
                loading: () => const CircularProgressIndicator(),
                error: (error, _) => Text('Error: $error'),
                data: (items) {
                  if (items.isEmpty) {
                    return const Text('Aún no hay aportes registrados.');
                  }
                  return Column(
                    children: items
                        .map(
                          (item) => ListTile(
                            title: Text(formatCop(item.amount)),
                            subtitle: Text('${item.scheduledDate} · ${item.statusLabel}'),
                            trailing: item.status == 'FAILED'
                                ? const Icon(Icons.error_outline, color: Colors.red)
                                : const Icon(Icons.check_circle_outline, color: Colors.green),
                          ),
                        )
                        .toList(),
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showPartialWithdrawalModal(BuildContext context, WidgetRef ref, dynamic account) {
    final maxAllowed = account.currentBalance * 0.40;
    final controller = TextEditingController();

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) {
        return Padding(
          padding: EdgeInsets.only(
            left: 24,
            right: 24,
            top: 24,
            bottom: MediaQuery.of(ctx).viewInsets.bottom + 24,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text('Retiro Parcial', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              Text('Puedes retirar hasta el 40% de tu saldo actual (${formatCop(maxAllowed)}).'),
              const SizedBox(height: 16),
              TextField(
                controller: controller,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'Monto a retirar',
                  prefixText: '\$ ',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: () async {
                  final amount = double.tryParse(controller.text) ?? 0.0;
                  if (amount <= 0 || amount > maxAllowed) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                      const SnackBar(content: Text('Monto inválido o excede el 40% permitido.')),
                    );
                    return;
                  }
                  Navigator.of(ctx).pop();
                  try {
                    await ref.read(scheduledSavingsListProvider.notifier).withdrawAccount(account.id, amount, 'PARTIAL');
                    ref.invalidate(scheduledSavingsDetailProvider(account.id));
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Retiro parcial exitoso.')));
                    }
                  } catch (e) {
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error: $e')));
                    }
                  }
                },
                child: const Text('Confirmar Retiro'),
              ),
            ],
          ),
        );
      },
    );
  }

  void _showFullWithdrawalConfirm(BuildContext context, WidgetRef ref, dynamic account) {
    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          title: const Text('Retirar todo y Cerrar'),
          content: const Text(
            'Esta acción extraerá todo tu dinero y cancelará tu meta de ahorro permanentemente. ¿Estás seguro?',
          ),
          actions: [
            TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('Cancelar')),
            TextButton(
              onPressed: () async {
                Navigator.of(ctx).pop();
                try {
                  await ref.read(scheduledSavingsListProvider.notifier).withdrawAccount(account.id, account.currentBalance, 'FULL');
                  ref.invalidate(scheduledSavingsDetailProvider(account.id));
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Cuenta cancelada y retiro exitoso.')));
                    Navigator.of(context).pop(); // Go back to list
                  }
                } catch (e) {
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error: $e')));
                  }
                }
              },
              child: const Text('Sí, retirar y cerrar', style: TextStyle(color: Colors.red)),
            ),
          ],
        );
      },
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(fontSize: 16)),
          Text(value, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}
