import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../core/ui/ui.dart';
import '../../../providers/admin_business_provider.dart';
import '../../shell/admin_nav.dart';

class AccountsPage extends ConsumerStatefulWidget {
  const AccountsPage({super.key});

  @override
  ConsumerState<AccountsPage> createState() => _AccountsPageState();
}

class _AccountsPageState extends ConsumerState<AccountsPage> {
  final _search = TextEditingController();
  String _query = '';
  String _status = 'ALL';

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  void _refresh() {
    ref.invalidate(accountsProvider);
    ref.invalidate(usersProvider);
  }

  @override
  Widget build(BuildContext context) {
    final accountsAsync = ref.watch(accountsWithUsersProvider);

    return AdminPage(
      section: sectionTitleFor(accountsItem.route),
      title: 'Cuentas y saldos',
      subtitle: 'Todas las cuentas de ahorro de la cooperativa, con su titular y saldo disponible.',
      actions: [
        OutlinedButton.icon(
          onPressed: _refresh,
          icon: const Icon(Icons.refresh_rounded, size: 18),
          label: const Text('Actualizar'),
        ),
      ],
      children: [
        accountsAsync.when(
          loading: () => const LoadingState(label: 'Cargando cuentas…'),
          error: (e, _) => ErrorState(error: e, onRetry: _refresh),
          data: _buildContent,
        ),
      ],
    );
  }

  Widget _buildContent(List<Map<String, dynamic>> rows) {
    if (rows.isEmpty) {
      return const EmptyState(
        icon: Icons.account_balance_wallet_outlined,
        title: 'Aún no hay cuentas',
        message: 'Las cuentas se crean cuando un socio completa su registro.',
      );
    }

    final accounts = rows.map((r) => r['account'] as AccountModel).toList();
    final total = accounts.fold<double>(0, (sum, a) => sum + a.balance);
    final active = accounts.where((a) => a.status == 'ACTIVE').length;
    int countOf(String s) => s == 'ALL' ? accounts.length : accounts.where((a) => a.status == s).length;

    final q = _query.trim().toLowerCase();
    final visible = rows.where((r) {
      final a = r['account'] as AccountModel;
      final u = r['user'] as UserModel;
      if (_status != 'ALL' && a.status != _status) return false;
      if (q.isEmpty) return true;
      return '${a.accountNumber} ${u.fullName} ${u.documentNumber}'.toLowerCase().contains(q);
    }).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ResponsiveGrid(
          minItemWidth: 160,
          maxColumns: 3,
          children: [
            StatCard(icon: Icons.account_balance_wallet_rounded, label: 'Cuentas', value: '${accounts.length}'),
            StatCard(icon: Icons.check_circle_rounded, label: 'Cuentas activas', value: '$active'),
            StatCard(icon: Icons.payments_rounded, label: 'Saldo total', value: formatCop(total), tone: Tone.info),
          ],
        ),
        const SizedBox(height: 24),
        LayoutBuilder(builder: (context, c) {
          final search = SearchField(
            controller: _search,
            hint: 'Buscar por cuenta, titular o documento',
            onChanged: (v) => setState(() => _query = v),
          );
          final filters = FilterBar<String>(
            selected: _status,
            onChanged: (s) => setState(() => _status = s),
            options: [
              FilterOption('ALL', 'Todas', count: countOf('ALL')),
              FilterOption('ACTIVE', 'Activas', count: countOf('ACTIVE')),
              FilterOption('BLOCKED', 'Bloqueadas', count: countOf('BLOCKED')),
              FilterOption('CLOSED', 'Cerradas', count: countOf('CLOSED')),
            ],
          );
          if (c.maxWidth < 900) {
            return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [search, const SizedBox(height: 12), filters]);
          }
          return Row(children: [Expanded(child: filters), const SizedBox(width: 16), SizedBox(width: 400, child: search)]);
        }),
        const SizedBox(height: 16),
        if (visible.isEmpty)
          Card(
            child: EmptyState(
              icon: Icons.search_off_rounded,
              title: q.isEmpty ? 'No hay cuentas en este estado' : 'Sin resultados para "$_query"',
            ),
          )
        else
          AdminTable<Map<String, dynamic>>(
            rows: visible,
            columns: [
              AdminColumn(
                label: 'Cuenta',
                flex: 3,
                cell: (r) => TwoLineCell((r['account'] as AccountModel).accountNumber, 'Ahorros',
                    leading: const IconBadge(Icons.account_balance_wallet_rounded, size: 36)),
              ),
              AdminColumn(
                label: 'Titular',
                flex: 3,
                cell: (r) {
                  final u = r['user'] as UserModel;
                  return TwoLineCell(u.fullName.isEmpty ? 'Titular desconocido' : u.fullName, u.documentLabel);
                },
              ),
              AdminColumn(
                label: 'Estado',
                flex: 2,
                cell: (r) => StatusBadge(accountStatus((r['account'] as AccountModel).status)),
              ),
              AdminColumn(
                label: 'Saldo',
                flex: 2,
                alignEnd: true,
                cell: (r) => CellText(formatCop((r['account'] as AccountModel).balance), strong: true),
              ),
            ],
          ),
      ],
    );
  }
}
