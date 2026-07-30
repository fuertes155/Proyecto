import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/widgets/accessible_button.dart';
import '../../data/models/external_bank_account_model.dart';
import '../providers/external_accounts_provider.dart';

class ExternalBankAccountsPage extends ConsumerStatefulWidget {
  const ExternalBankAccountsPage({super.key});

  @override
  ConsumerState<ExternalBankAccountsPage> createState() => _ExternalBankAccountsPageState();
}

class _ExternalBankAccountsPageState extends ConsumerState<ExternalBankAccountsPage> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(externalBankAccountsListProvider.notifier).load());
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(externalBankAccountsListProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Cuentas bancarias')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () async {
          await context.push('/accounts/external/add');
          ref.read(externalBankAccountsListProvider.notifier).load();
        },
        icon: const Icon(Icons.add),
        label: const Text('Agregar cuenta'),
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
                onPressed: () => ref.read(externalBankAccountsListProvider.notifier).load(),
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
                  const Icon(Icons.account_balance_outlined, size: 72),
                  const SizedBox(height: 16),
                  const Text(
                    'Aún no tienes cuentas bancarias registradas',
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Registra una cuenta a tu propio nombre para poder retirar tu saldo.',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 16),
                  ),
                ],
              ),
            );
          }

          return RefreshIndicator(
            onRefresh: () => ref.read(externalBankAccountsListProvider.notifier).load(),
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

class _AccountCard extends ConsumerWidget {
  const _AccountCard({required this.account});

  final ExternalBankAccountModel account;

  Future<void> _resendVerification(BuildContext context, WidgetRef ref) async {
    try {
      await ref.read(externalBankAccountsListProvider.notifier).resendVerification(account.id);
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Depósito de verificación enviado. Revisa tu extracto bancario.')),
      );
    } on DioException catch (e) {
      if (!context.mounted) return;
      String message = 'No fue posible enviar el depósito de verificación';
      if (e.response?.data is Map && (e.response!.data as Map).containsKey('message')) {
        message = (e.response!.data as Map)['message'] as String;
      }
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
    }
  }

  Future<void> _confirmVerification(BuildContext context, WidgetRef ref) async {
    final controller = TextEditingController();
    final amount = await showDialog<int>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Confirma el depósito'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Enviamos un depósito pequeño a ${account.bankName} (${account.maskedAccountNumber}). '
                'Ingresa el monto exacto que viste en tu extracto para confirmar que la cuenta es tuya.'),
            const SizedBox(height: 16),
            TextField(
              controller: controller,
              autofocus: true,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              decoration: const InputDecoration(labelText: 'Monto recibido (\$)'),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, int.tryParse(controller.text)),
            child: const Text('Confirmar'),
          ),
        ],
      ),
    );

    if (amount == null) return;

    try {
      await ref.read(externalBankAccountsListProvider.notifier).confirmVerification(account.id, amount);
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('¡Cuenta verificada!')),
      );
    } on DioException catch (e) {
      if (!context.mounted) return;
      String message = 'El monto ingresado no coincide';
      if (e.response?.data is Map && (e.response!.data as Map).containsKey('message')) {
        message = (e.response!.data as Map)['message'] as String;
      }
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    account.bankName,
                    style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
                  ),
                ),
                _StatusChip(status: account.verificationStatus, label: account.statusLabel),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              '${account.accountTypeLabel} · ${account.maskedAccountNumber}',
              style: TextStyle(fontSize: 15, color: Colors.grey.shade700),
            ),
            if (account.verificationStatus == 'PENDING') ...[
              const SizedBox(height: 12),
              Align(
                alignment: Alignment.centerRight,
                child: account.verificationPending
                    ? TextButton.icon(
                        icon: const Icon(Icons.verified_outlined, size: 18),
                        label: const Text('Confirmar depósito'),
                        onPressed: () => _confirmVerification(context, ref),
                      )
                    : TextButton.icon(
                        icon: const Icon(Icons.send_outlined, size: 18),
                        label: const Text('Enviar verificación'),
                        onPressed: () => _resendVerification(context, ref),
                      ),
              ),
            ],
            if (account.isUsable) ...[
              const SizedBox(height: 4),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton.icon(
                  icon: const Icon(Icons.arrow_upward, size: 18),
                  label: const Text('Retirar a esta cuenta'),
                  onPressed: () => context.push('/payout', extra: {'accountId': account.id}),
                ),
              ),
            ],
          ],
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
      'VERIFIED' => Colors.green,
      'PENDING' => Colors.orange,
      'REJECTED' => Colors.red,
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
