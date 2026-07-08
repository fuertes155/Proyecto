import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../providers/admin_business_provider.dart';

class AccountsPage extends ConsumerWidget {
  const AccountsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final accountsAsync = ref.watch(accountsProvider);
    final primary = Theme.of(context).colorScheme.primary;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Cuentas y Saldos', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
      ),
      body: accountsAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(child: Text('Error: $err')),
        data: (accounts) {
          if (accounts.isEmpty) {
            return const Center(child: Text('No hay cuentas creadas en el núcleo bancario.'));
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: accounts.length,
            itemBuilder: (context, index) {
              final account = accounts[index];
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
                    child: Icon(Icons.account_balance_wallet, color: primary),
                  ),
                  title: Text('Cuenta: ${account.accountNumber}', style: const TextStyle(fontWeight: FontWeight.w600)),
                  subtitle: Text('Propietario ID: ${account.userId}'),
                  trailing: Chip(
                    label: Text(account.status, style: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold)),
                    backgroundColor: account.status == 'ACTIVE' ? Colors.green.withOpacity(0.2) : Colors.red.withOpacity(0.2),
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
