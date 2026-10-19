import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../core/ui/ui.dart';
import '../../../providers/admin_business_provider.dart';
import '../../shell/admin_nav.dart';

enum _KycFilter { all, pending, approved, rejected }

class PartnersPage extends ConsumerStatefulWidget {
  const PartnersPage({super.key});

  @override
  ConsumerState<PartnersPage> createState() => _PartnersPageState();
}

class _PartnersPageState extends ConsumerState<PartnersPage> {
  final _search = TextEditingController();
  String _query = '';
  _KycFilter _filter = _KycFilter.pending;
  bool _filterTouched = false;

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  bool _matchesFilter(UserModel u, _KycFilter f) => switch (f) {
        _KycFilter.all => true,
        _KycFilter.pending => isKycPending(u.kycStatus),
        _KycFilter.approved => u.kycStatus == 'APPROVED',
        _KycFilter.rejected => u.kycStatus == 'REJECTED',
      };

  Future<void> _review(UserModel user, String targetStatus) async {
    final approve = targetStatus == 'APPROVED';
    final confirmed = await confirmAction(
      context,
      title: approve ? 'Aprobar KYC' : 'Denegar KYC',
      message: approve
          ? 'Confirmas que la identidad del socio fue validada. Podrá depositar y operar de inmediato.'
          : 'El socio no podrá depositar ni operar hasta que su KYC sea aprobado.',
      confirmLabel: approve ? 'Aprobar' : 'Denegar',
      destructive: !approve,
      icon: approve ? Icons.how_to_reg_rounded : Icons.person_off_rounded,
      details: [
        ('Socio', user.fullName),
        ('Documento', user.documentLabel),
        if (user.email.isNotEmpty) ('Correo', user.email),
      ],
    );
    if (!confirmed || !mounted) return;

    try {
      await ref.read(adminApiClientProvider).put('/users/${user.id}/kyc?status=$targetStatus');
      ref.invalidate(usersProvider);
      if (mounted) {
        showMetSnack(context, approve ? 'KYC de ${user.firstName} aprobado' : 'KYC de ${user.firstName} denegado',
            tone: approve ? Tone.success : Tone.neutral);
      }
    } catch (e) {
      if (mounted) showErrorSnack(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final usersAsync = ref.watch(usersProvider);

    return AdminPage(
      section: sectionTitleFor(partnersItem.route),
      title: 'Socios y KYC',
      subtitle: 'Revisa la identidad de quienes se registran y decide si pueden empezar a operar.',
      actions: [
        OutlinedButton.icon(
          onPressed: () => ref.invalidate(usersProvider),
          icon: const Icon(Icons.refresh_rounded, size: 18),
          label: const Text('Actualizar'),
        ),
      ],
      children: [
        usersAsync.when(
          loading: () => const LoadingState(label: 'Cargando socios…'),
          error: (e, _) => ErrorState(error: e, onRetry: () => ref.invalidate(usersProvider)),
          data: (users) => _buildContent(users),
        ),
      ],
    );
  }

  Widget _buildContent(List<UserModel> users) {
    if (users.isEmpty) {
      return const EmptyState(
        icon: Icons.people_outline_rounded,
        title: 'Aún no hay socios registrados',
        message: 'Cuando alguien se registre desde la app aparecerá aquí para validar su identidad.',
      );
    }

    final counts = {for (final f in _KycFilter.values) f: users.where((u) => _matchesFilter(u, f)).length};
    // Si no hay pendientes, abrir en "Todos" para no mostrar una tabla vacía.
    final filter = !_filterTouched && counts[_KycFilter.pending] == 0 ? _KycFilter.all : _filter;

    final q = _query.trim().toLowerCase();
    final visible = users.where((u) {
      if (!_matchesFilter(u, filter)) return false;
      if (q.isEmpty) return true;
      return '${u.fullName} ${u.documentNumber} ${u.email}'.toLowerCase().contains(q);
    }).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ResponsiveGrid(
          minItemWidth: 160,
          children: [
            StatCard(icon: Icons.people_alt_rounded, label: 'Socios registrados', value: '${users.length}'),
            StatCard(
              icon: Icons.hourglass_top_rounded,
              label: 'KYC por revisar',
              value: '${counts[_KycFilter.pending]}',
              tone: counts[_KycFilter.pending]! > 0 ? Tone.warning : Tone.success,
            ),
            StatCard(icon: Icons.verified_rounded, label: 'Aprobados', value: '${counts[_KycFilter.approved]}'),
            StatCard(
              icon: Icons.block_rounded,
              label: 'Rechazados',
              value: '${counts[_KycFilter.rejected]}',
              tone: Tone.neutral,
            ),
          ],
        ),
        const SizedBox(height: 24),
        LayoutBuilder(builder: (context, c) {
          final search = SearchField(
            controller: _search,
            hint: 'Buscar por nombre, documento o correo',
            onChanged: (v) => setState(() => _query = v),
          );
          final filters = FilterBar<_KycFilter>(
            selected: filter,
            onChanged: (f) => setState(() {
              _filter = f;
              _filterTouched = true;
            }),
            options: [
              FilterOption(_KycFilter.pending, 'Por revisar', count: counts[_KycFilter.pending]),
              FilterOption(_KycFilter.approved, 'Aprobados', count: counts[_KycFilter.approved]),
              FilterOption(_KycFilter.rejected, 'Rechazados', count: counts[_KycFilter.rejected]),
              FilterOption(_KycFilter.all, 'Todos', count: counts[_KycFilter.all]),
            ],
          );
          if (c.maxWidth < 900) {
            return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [search, const SizedBox(height: 12), filters]);
          }
          return Row(children: [Expanded(child: filters), const SizedBox(width: 16), SizedBox(width: 380, child: search)]);
        }),
        const SizedBox(height: 16),
        if (visible.isEmpty)
          Card(
            child: EmptyState(
              icon: q.isEmpty ? Icons.task_alt_rounded : Icons.search_off_rounded,
              title: q.isEmpty ? 'Nada pendiente en este filtro' : 'Sin resultados para "$_query"',
              message: q.isEmpty ? null : 'Prueba con otro nombre o número de documento.',
            ),
          )
        else
          AdminTable<UserModel>(
            rows: visible,
            columns: [
              AdminColumn(
                label: 'Socio',
                flex: 4,
                cell: (u) => TwoLineCell(u.fullName, u.email.isEmpty ? 'Sin correo registrado' : u.email,
                    leading: InitialsAvatar(u.fullName)),
              ),
              AdminColumn(label: 'Documento', flex: 2, cell: (u) => CellText(u.documentLabel)),
              AdminColumn(
                label: 'Registro',
                flex: 2,
                cell: (u) => CellText(formatDateTime(u.createdAt, withTime: false), muted: true),
              ),
              AdminColumn(label: 'Estado KYC', flex: 2, cell: (u) => StatusBadge(kycStatus(u.kycStatus))),
              AdminColumn(
                label: 'Acciones',
                flex: 3,
                alignEnd: true,
                cell: (u) => _Actions(user: u, onReview: _review),
              ),
            ],
          ),
      ],
    );
  }
}

class _Actions extends StatelessWidget {
  const _Actions({required this.user, required this.onReview});

  final UserModel user;
  final Future<void> Function(UserModel, String) onReview;

  @override
  Widget build(BuildContext context) {
    if (user.kycStatus == 'APPROVED') {
      return const Text('Sin acciones', style: TextStyle(fontSize: 13, color: MetColors.textFaint));
    }
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        if (user.kycStatus != 'REJECTED')
          TextButton(
            onPressed: () => onReview(user, 'REJECTED'),
            style: TextButton.styleFrom(foregroundColor: MetColors.danger),
            child: const Text('Denegar'),
          ),
        FilledButton.icon(
          onPressed: () => onReview(user, 'APPROVED'),
          icon: const Icon(Icons.check_rounded, size: 18),
          label: const Text('Aprobar'),
        ),
      ],
    );
  }
}
