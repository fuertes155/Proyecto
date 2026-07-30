import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../providers/admin_business_provider.dart';

class AccountsPage extends ConsumerStatefulWidget {
  const AccountsPage({super.key});

  @override
  ConsumerState<AccountsPage> createState() => _AccountsPageState();
}

class _AccountsPageState extends ConsumerState<AccountsPage> {
  final _searchController = TextEditingController();
  String _query = '';

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final accountsAsync = ref.watch(accountsWithUsersProvider);
    final primary = Theme.of(context).colorScheme.primary;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Cuentas y Saldos', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
            child: TextField(
              controller: _searchController,
              onChanged: (value) => setState(() => _query = value),
              style: TextStyle(color: Theme.of(context).colorScheme.onSurface),
              decoration: InputDecoration(
                hintText: 'Buscar por número de cuenta, propietario o cédula...',
                prefixIcon: Icon(Icons.search, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4)),
                suffixIcon: _query.isEmpty
                    ? null
                    : IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () => setState(() {
                          _searchController.clear();
                          _query = '';
                        }),
                      ),
                filled: true,
                fillColor: Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.05 : 0.02),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(16),
                  borderSide: BorderSide.none,
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(16),
                  borderSide: BorderSide(color: Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.12 : 0.05)),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(16),
                  borderSide: BorderSide(color: primary),
                ),
              ),
            ),
          ),
          Expanded(
            child: accountsAsync.when(
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (err, stack) => Center(child: Text('Error: $err')),
              data: (accounts) {
                if (accounts.isEmpty) {
                  return const Center(child: Text('No hay cuentas creadas en el núcleo bancario.'));
                }
                final query = _query.trim().toLowerCase();
                final filtered = query.isEmpty
                    ? accounts
                    : accounts.where((data) {
                        final account = data['account'] as AccountModel;
                        final user = data['user'] as UserModel;
                        final haystack = '${account.accountNumber} ${user.firstName} ${user.lastName} ${user.documentNumber}'.toLowerCase();
                        return haystack.contains(query);
                      }).toList();
                if (filtered.isEmpty) {
                  return const Center(child: Text('Ningún resultado coincide con la búsqueda.'));
                }
                return ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: filtered.length,
                  itemBuilder: (context, index) {
                    final data = filtered[index];
                    final account = data['account'] as AccountModel;
                    final user = data['user'] as UserModel;
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
                        subtitle: Text('Propietario: ${user.firstName} ${user.lastName}\nCC: ${user.documentNumber}'),
                        isThreeLine: true,
                        trailing: Column(
                          mainAxisSize: MainAxisSize.min,
                          mainAxisAlignment: MainAxisAlignment.center,
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text(
                              NumberFormat.currency(symbol: '\$', decimalDigits: 2, locale: 'en_US').format(account.balance),
                              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)
                            ),
                            Chip(
                              label: Text(account.status, style: const TextStyle(fontSize: 9, fontWeight: FontWeight.bold)),
                              backgroundColor: account.status == 'ACTIVE' ? Colors.green.withOpacity(0.2) : Colors.red.withOpacity(0.2),
                              side: BorderSide.none,
                              padding: EdgeInsets.zero,
                              materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                              visualDensity: VisualDensity.compact,
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
