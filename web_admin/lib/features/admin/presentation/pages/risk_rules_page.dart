import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/ui/ui.dart';
import '../../data/models/admin_models.dart';
import '../providers/admin_provider.dart';
import '../shell/admin_nav.dart';

class RiskRulesPage extends ConsumerWidget {
  const RiskRulesPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final rulesState = ref.watch(riskRulesProvider);
    final active = rulesState.valueOrNull?.where((r) => r.activo).length;
    final total = rulesState.valueOrNull?.length;

    return AdminPage(
      section: sectionTitleFor(riskRulesItem.route),
      title: 'Reglas de riesgo',
      subtitle: active == null
          ? 'Reglas antifraude que revisan o bloquean operaciones automáticamente.'
          : '$active de $total reglas activas revisando o bloqueando operaciones en tiempo real.',
      actions: [
        OutlinedButton.icon(
          onPressed: () => ref.read(riskRulesProvider.notifier).load(),
          icon: const Icon(Icons.refresh_rounded, size: 18),
          label: const Text('Actualizar'),
        ),
      ],
      children: [
        rulesState.when(
          loading: () => const LoadingState(label: 'Cargando reglas…'),
          error: (e, _) => ErrorState(error: e, onRetry: () => ref.read(riskRulesProvider.notifier).load()),
          data: (rules) {
            if (rules.isEmpty) {
              return const EmptyState(icon: Icons.policy_outlined, title: 'No hay reglas configuradas');
            }
            return ResponsiveGrid(
              minItemWidth: 420,
              maxColumns: 2,
              children: [for (final rule in rules) _RiskRuleCard(rule: rule)],
            );
          },
        ),
      ],
    );
  }
}

class _RiskRuleCard extends ConsumerStatefulWidget {
  const _RiskRuleCard({required this.rule});

  final RiskRule rule;

  @override
  ConsumerState<_RiskRuleCard> createState() => _RiskRuleCardState();
}

class _RiskRuleCardState extends ConsumerState<_RiskRuleCard> {
  bool _saving = false;

  Future<void> _toggle(bool value) async {
    if (!value) {
      final confirmed = await confirmAction(
        context,
        title: 'Pausar regla',
        message: 'Mientras esté pausada, las operaciones que cumplan esta condición no se revisarán ni bloquearán.',
        confirmLabel: 'Pausar regla',
        destructive: true,
        icon: Icons.pause_circle_outline_rounded,
        details: [('Regla', widget.rule.nombre)],
      );
      if (!confirmed) return;
    }
    setState(() => _saving = true);
    try {
      await ref.read(riskRulesProvider.notifier).toggle(widget.rule.id, value);
      if (mounted) {
        showMetSnack(context, value ? 'Regla "${widget.rule.nombre}" activada' : 'Regla "${widget.rule.nombre}" pausada',
            tone: value ? Tone.success : Tone.neutral);
      }
    } catch (e) {
      if (mounted) showErrorSnack(context, e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final rule = widget.rule;
    final action = _describeAction(rule.accion);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                IconBadge(Icons.policy_rounded, tone: rule.activo ? Tone.success : Tone.neutral),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(rule.nombre, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
                      const SizedBox(height: 2),
                      Text(rule.descripcion ?? 'Sin descripción',
                          style: const TextStyle(fontSize: 13.5, color: MetColors.textMuted, height: 1.4)),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                if (_saving)
                  const Padding(
                    padding: EdgeInsets.all(12),
                    child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)),
                  )
                else
                  Tooltip(
                    message: rule.activo ? 'Pausar regla' : 'Activar regla',
                    child: Switch(value: rule.activo, onChanged: _toggle),
                  ),
              ],
            ),
            const SizedBox(height: 16),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(color: MetColors.subtle, borderRadius: BorderRadius.circular(12)),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('SE ACTIVA CUANDO',
                      style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.w700, letterSpacing: 0.8, color: MetColors.textMuted)),
                  const SizedBox(height: 4),
                  Text(_describeCondition(rule.condicion),
                      style: const TextStyle(fontSize: 14.5, fontWeight: FontWeight.w600)),
                ],
              ),
            ),
            const SizedBox(height: 14),
            Row(
              children: [
                const Text('Acción', style: TextStyle(fontSize: 13.5, color: MetColors.textMuted)),
                const SizedBox(width: 10),
                StatusBadge(action, showDot: false),
                const Spacer(),
                StatusBadge(rule.activo ? const StatusInfo('Activa', Tone.success) : const StatusInfo('Pausada', Tone.neutral)),
              ],
            ),
          ],
        ),
      ),
    );
  }

  static StatusInfo _describeAction(String action) => switch (action) {
        'REVIEW' => const StatusInfo('Revisión manual', Tone.warning),
        'BLOCK' => const StatusInfo('Bloqueo automático', Tone.danger),
        _ => StatusInfo(action, Tone.neutral),
      };

  /// {"field":"amount","operator":"gt","value":10000000} → "Monto mayor a $10.000.000".
  static String _describeCondition(String raw) {
    try {
      final map = jsonDecode(raw) as Map<String, dynamic>;
      final field = map['field'];
      final value = map['value'];
      final op = switch (map['operator']) {
        'gt' => 'mayor a',
        'gte' => 'mayor o igual a',
        'lt' => 'menor a',
        'lte' => 'menor o igual a',
        'eq' => 'igual a',
        final other => other.toString(),
      };
      final window = map.containsKey('window_minutes') ? ' en ${map['window_minutes']} minutos' : '';
      if (field == 'amount' && value is num) return 'Monto $op ${formatCop(value.toDouble())}$window';
      final fieldText = switch (field) {
        'amount' => 'Monto',
        'failed_attempts' => 'Intentos fallidos',
        final other => other.toString(),
      };
      return '$fieldText $op $value$window';
    } catch (_) {
      return raw;
    }
  }
}
