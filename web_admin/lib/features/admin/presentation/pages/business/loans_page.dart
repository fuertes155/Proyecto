import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../core/ui/ui.dart';
import '../../../providers/admin_business_provider.dart';
import '../../shell/admin_nav.dart';

enum _LoanFilter { awaiting, approved, rejected, all }

StatusInfo _riskTier(String? tier) => switch (tier) {
      'PRIME' => const StatusInfo('Prime', Tone.success),
      'RIESGO_BAJO' => const StatusInfo('Riesgo bajo', Tone.success),
      'RIESGO_MEDIO' => const StatusInfo('Riesgo medio', Tone.warning),
      'RIESGO_ALTO' => const StatusInfo('Riesgo alto', Tone.danger),
      'RECHAZADO' => const StatusInfo('No elegible', Tone.danger),
      null || '' => const StatusInfo('Sin evaluar', Tone.neutral),
      _ => StatusInfo(tier, Tone.neutral),
    };

class LoansPage extends ConsumerStatefulWidget {
  const LoansPage({super.key});

  @override
  ConsumerState<LoansPage> createState() => _LoansPageState();
}

class _LoansPageState extends ConsumerState<LoansPage> {
  _LoanFilter _filter = _LoanFilter.awaiting;
  bool _filterTouched = false;

  bool _matches(LoanModel l, _LoanFilter f) => switch (f) {
        _LoanFilter.awaiting => isLoanAwaitingDecision(l.status),
        _LoanFilter.approved => l.status == 'APPROVED' || l.status == 'DISBURSED' || l.status == 'PAID_OFF',
        _LoanFilter.rejected => l.status == 'REJECTED',
        _LoanFilter.all => true,
      };

  void _refresh() {
    ref.invalidate(loansProvider);
    ref.invalidate(usersProvider);
  }

  Future<void> _decide(LoanModel loan, UserModel user, bool approve) async {
    final confirmed = await confirmAction(
      context,
      title: approve ? 'Aprobar crédito' : 'Rechazar crédito',
      message: approve
          ? 'El crédito quedará aprobado y el socio será notificado.'
          : 'La solicitud se cerrará como rechazada. El socio podrá presentar una nueva más adelante.',
      confirmLabel: approve ? 'Aprobar crédito' : 'Rechazar',
      destructive: !approve,
      icon: approve ? Icons.task_alt_rounded : Icons.cancel_outlined,
      details: [
        ('Solicitante', user.fullName),
        ('Documento', user.documentLabel),
        if (loan.amount != null) ('Monto', formatCop(loan.amount!)),
        if (loan.termMonths != null) ('Plazo', '${loan.termMonths} meses'),
        ('Riesgo', _riskTier(loan.riskTier).label),
      ],
    );
    if (!confirmed || !mounted) return;

    try {
      await ref.read(adminApiClientProvider).put('/loans/${loan.id}/status?status=${approve ? 'APPROVED' : 'REJECTED'}');
      ref.invalidate(loansProvider);
      if (mounted) {
        showMetSnack(context, approve ? 'Crédito de ${user.firstName} aprobado' : 'Crédito de ${user.firstName} rechazado',
            tone: approve ? Tone.success : Tone.neutral);
      }
    } catch (e) {
      if (mounted) showErrorSnack(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final loansAsync = ref.watch(loansWithUsersProvider);

    return AdminPage(
      section: sectionTitleFor(loansItem.route),
      title: 'Solicitudes de crédito',
      subtitle: 'Evalúa cada solicitud con su monto, plazo y nivel de riesgo antes de decidir.',
      actions: [
        OutlinedButton.icon(
          onPressed: _refresh,
          icon: const Icon(Icons.refresh_rounded, size: 18),
          label: const Text('Actualizar'),
        ),
      ],
      children: [
        loansAsync.when(
          loading: () => const LoadingState(label: 'Cargando solicitudes…'),
          error: (e, _) => ErrorState(error: e, onRetry: _refresh),
          data: _buildContent,
        ),
      ],
    );
  }

  Widget _buildContent(List<Map<String, dynamic>> rows) {
    if (rows.isEmpty) {
      return const EmptyState(
        icon: Icons.request_quote_outlined,
        title: 'No hay solicitudes de crédito',
        message: 'Las solicitudes que los socios hagan desde la app aparecerán aquí.',
      );
    }

    final loans = rows.map((r) => r['loan'] as LoanModel).toList();
    final counts = {for (final f in _LoanFilter.values) f: loans.where((l) => _matches(l, f)).length};
    final filter = !_filterTouched && counts[_LoanFilter.awaiting] == 0 ? _LoanFilter.all : _filter;
    final visible = rows.where((r) => _matches(r['loan'] as LoanModel, filter)).toList();
    final requested = loans.where((l) => isLoanAwaitingDecision(l.status)).fold<double>(0, (s, l) => s + (l.amount ?? 0));

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ResponsiveGrid(
          minItemWidth: 160,
          maxColumns: 3,
          children: [
            StatCard(
              icon: Icons.hourglass_top_rounded,
              label: 'Por decidir',
              value: '${counts[_LoanFilter.awaiting]}',
              tone: counts[_LoanFilter.awaiting]! > 0 ? Tone.warning : Tone.success,
            ),
            StatCard(icon: Icons.payments_rounded, label: 'Monto solicitado por decidir', value: formatCop(requested), tone: Tone.info),
            StatCard(icon: Icons.task_alt_rounded, label: 'Aprobadas', value: '${counts[_LoanFilter.approved]}'),
          ],
        ),
        const SizedBox(height: 24),
        FilterBar<_LoanFilter>(
          selected: filter,
          onChanged: (f) => setState(() {
            _filter = f;
            _filterTouched = true;
          }),
          options: [
            FilterOption(_LoanFilter.awaiting, 'Por decidir', count: counts[_LoanFilter.awaiting]),
            FilterOption(_LoanFilter.approved, 'Aprobadas', count: counts[_LoanFilter.approved]),
            FilterOption(_LoanFilter.rejected, 'Rechazadas', count: counts[_LoanFilter.rejected]),
            FilterOption(_LoanFilter.all, 'Todas', count: counts[_LoanFilter.all]),
          ],
        ),
        const SizedBox(height: 16),
        if (visible.isEmpty)
          const Card(
            child: EmptyState(icon: Icons.task_alt_rounded, title: 'No hay solicitudes en este filtro'),
          )
        else
          AdminTable<Map<String, dynamic>>(
            rows: visible,
            columns: [
              AdminColumn(
                label: 'Solicitante',
                flex: 4,
                cell: (r) {
                  final u = r['user'] as UserModel;
                  final l = r['loan'] as LoanModel;
                  return TwoLineCell(
                    u.fullName.isEmpty ? 'Socio desconocido' : u.fullName,
                    'Solicitud ${shortId(l.id)} · ${formatDateTime(l.submittedAt, withTime: false)}',
                    leading: InitialsAvatar(u.fullName),
                  );
                },
              ),
              AdminColumn(
                label: 'Monto y plazo',
                flex: 3,
                cell: (r) {
                  final l = r['loan'] as LoanModel;
                  return TwoLineCell(
                    l.amount == null ? '—' : formatCop(l.amount!),
                    [
                      if (l.termMonths != null) '${l.termMonths} meses',
                      if (l.monthlyPayment != null) 'cuota ${formatCop(l.monthlyPayment!)}',
                    ].join(' · '),
                  );
                },
              ),
              AdminColumn(
                label: 'Propósito',
                flex: 2,
                showOnCompact: false,
                cell: (r) => CellText((r['loan'] as LoanModel).purpose ?? '—', muted: true),
              ),
              AdminColumn(
                label: 'Riesgo',
                flex: 2,
                cell: (r) {
                  final l = r['loan'] as LoanModel;
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      StatusBadge(_riskTier(l.riskTier), showDot: false),
                      if (l.creditScore != null)
                        Padding(
                          padding: const EdgeInsets.only(top: 4, left: 2),
                          child: Text('Puntaje ${l.creditScore}',
                              style: const TextStyle(fontSize: 12.5, color: MetColors.textMuted)),
                        ),
                    ],
                  );
                },
              ),
              AdminColumn(
                label: 'Estado',
                flex: 2,
                cell: (r) => StatusBadge(loanStatus((r['loan'] as LoanModel).status)),
              ),
              AdminColumn(
                label: 'Acciones',
                flex: 4,
                alignEnd: true,
                cell: (r) {
                  final l = r['loan'] as LoanModel;
                  final u = r['user'] as UserModel;
                  if (!isLoanAwaitingDecision(l.status)) {
                    return const Text('Decidida', style: TextStyle(fontSize: 13, color: MetColors.textFaint));
                  }
                  return Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      TextButton(
                        onPressed: () => _decide(l, u, false),
                        style: TextButton.styleFrom(foregroundColor: MetColors.danger),
                        child: const Text('Rechazar'),
                      ),
                      FilledButton.icon(
                        onPressed: () => _decide(l, u, true),
                        icon: const Icon(Icons.check_rounded, size: 18),
                        label: const Text('Aprobar'),
                      ),
                    ],
                  );
                },
              ),
            ],
          ),
      ],
    );
  }
}
