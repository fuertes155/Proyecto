import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/ui/ui.dart';
import '../../data/models/admin_models.dart';
import '../providers/admin_provider.dart';
import '../shell/admin_nav.dart';

class GuaranteeFundPage extends ConsumerWidget {
  const GuaranteeFundPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(guaranteeFundProvider);

    return AdminPage(
      section: sectionTitleFor(guaranteeFundItem.route),
      title: 'Fondo de garantías',
      subtitle: 'Respaldo que cubre parte del capital de los inversionistas cuando un crédito entra en mora sostenida.',
      actions: [
        OutlinedButton.icon(
          onPressed: () => ref.read(guaranteeFundProvider.notifier).load(),
          icon: const Icon(Icons.refresh_rounded, size: 18),
          label: const Text('Actualizar'),
        ),
      ],
      children: [
        state.when(
          loading: () => const LoadingState(label: 'Cargando fondo…'),
          error: (e, _) => ErrorState(error: e, onRetry: () => ref.read(guaranteeFundProvider.notifier).load()),
          data: (summary) => _Content(summary: summary),
        ),
      ],
    );
  }
}

class _Content extends StatelessWidget {
  const _Content({required this.summary});

  final GuaranteeFundSummary summary;

  @override
  Widget build(BuildContext context) {
    final contributions = summary.movements.where((m) => m.isContribution).fold<double>(0, (s, m) => s + m.amount);
    final payouts = summary.movements.where((m) => !m.isContribution).fold<double>(0, (s, m) => s + m.amount);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          padding: const EdgeInsets.all(28),
          decoration: BoxDecoration(gradient: MetColors.brandGradient, borderRadius: BorderRadius.circular(20)),
          child: LayoutBuilder(builder: (context, c) {
            final balance = Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Saldo disponible',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.75), fontSize: 14.5, fontWeight: FontWeight.w600)),
                const SizedBox(height: 6),
                FittedBox(
                  fit: BoxFit.scaleDown,
                  alignment: Alignment.centerLeft,
                  child: Text(formatCop(summary.balance),
                      style: const TextStyle(color: Colors.white, fontSize: 40, fontWeight: FontWeight.w800, letterSpacing: -1)),
                ),
              ],
            );
            final rules = Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                _HeroStat(
                  icon: Icons.pie_chart_rounded,
                  label: 'Cobertura máxima',
                  value: '${(summary.coverageRatio * 100).toStringAsFixed(0)}% del saldo en mora',
                ),
                _HeroStat(
                  icon: Icons.event_busy_rounded,
                  label: 'Se activa a partir de',
                  value: '${summary.activationDaysLate} días de mora',
                ),
              ],
            );
            if (c.maxWidth < 820) {
              return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [balance, const SizedBox(height: 20), rules]);
            }
            return Row(children: [Expanded(child: balance), rules]);
          }),
        ),
        const SizedBox(height: 20),
        ResponsiveGrid(
          minItemWidth: 260,
          maxColumns: 2,
          children: [
            StatCard(icon: Icons.add_circle_outline_rounded, label: 'Aportes recibidos', value: formatCop(contributions)),
            StatCard(
              icon: Icons.remove_circle_outline_rounded,
              label: 'Coberturas pagadas',
              value: formatCop(payouts),
              tone: Tone.warning,
            ),
          ],
        ),
        const SizedBox(height: 28),
        Text('Movimientos', style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: 12),
        if (summary.movements.isEmpty)
          const Card(
            child: EmptyState(
              icon: Icons.receipt_long_outlined,
              title: 'Todavía no hay movimientos',
              message: 'Los aportes llegan con cada crédito desembolsado.',
            ),
          )
        else
          AdminTable<GuaranteeFundMovement>(
            rows: summary.movements,
            columns: [
              AdminColumn(
                label: 'Concepto',
                flex: 5,
                cell: (m) => TwoLineCell(
                  m.concept,
                  m.transactionReference,
                  leading: IconBadge(m.isContribution ? Icons.south_west_rounded : Icons.north_east_rounded,
                      tone: m.isContribution ? Tone.success : Tone.warning, size: 34),
                ),
              ),
              AdminColumn(
                label: 'Tipo',
                flex: 2,
                cell: (m) => StatusBadge(m.isContribution
                    ? const StatusInfo('Aporte', Tone.success)
                    : const StatusInfo('Cobertura', Tone.warning)),
              ),
              AdminColumn(label: 'Fecha', flex: 2, cell: (m) => CellText(formatDateTime(m.createdAt), muted: true)),
              AdminColumn(
                label: 'Monto',
                flex: 2,
                alignEnd: true,
                cell: (m) => Text(
                  '${m.isContribution ? '+' : '−'}${formatCop(m.amount)}',
                  style: TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: 14.5,
                    color: m.isContribution ? MetColors.success : MetColors.warning,
                  ),
                ),
              ),
            ],
          ),
      ],
    );
  }
}

class _HeroStat extends StatelessWidget {
  const _HeroStat({required this.icon, required this.label, required this.value});

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 240,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.white.withValues(alpha: 0.16)),
      ),
      child: Row(
        children: [
          Icon(icon, color: Colors.white, size: 22),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12.5)),
                Text(value, style: const TextStyle(color: Colors.white, fontSize: 14.5, fontWeight: FontWeight.w700)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
