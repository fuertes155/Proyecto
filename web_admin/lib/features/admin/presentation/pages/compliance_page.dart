import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/ui/ui.dart';
import '../../data/models/admin_models.dart';
import '../providers/admin_provider.dart';
import '../shell/admin_nav.dart';

enum _ComplianceTab { alerts, lists }

/// Panel de cumplimiento SARLAFT: alertas de operaciones inusuales
/// (monto/frecuencia/patrón) y coincidencias contra listas restrictivas
/// (OFAC/ONU).
class CompliancePage extends ConsumerStatefulWidget {
  const CompliancePage({super.key});

  @override
  ConsumerState<CompliancePage> createState() => _CompliancePageState();
}

class _CompliancePageState extends ConsumerState<CompliancePage> {
  _ComplianceTab _tab = _ComplianceTab.alerts;

  @override
  Widget build(BuildContext context) {
    return AdminPage(
      section: sectionTitleFor(complianceItem.route),
      title: 'Cumplimiento SARLAFT',
      subtitle: 'Revisa operaciones inusuales y coincidencias con listas restrictivas (OFAC y ONU).',
      children: [
        Align(
          alignment: Alignment.centerLeft,
          child: SegmentedButton<_ComplianceTab>(
            showSelectedIcon: false,
            segments: const [
              ButtonSegment(
                value: _ComplianceTab.alerts,
                icon: Icon(Icons.notifications_active_outlined, size: 18),
                label: Text('Alertas de operaciones'),
              ),
              ButtonSegment(
                value: _ComplianceTab.lists,
                icon: Icon(Icons.fact_check_outlined, size: 18),
                label: Text('Listas restrictivas'),
              ),
            ],
            selected: {_tab},
            onSelectionChanged: (s) => setState(() => _tab = s.first),
            style: SegmentedButton.styleFrom(
              selectedBackgroundColor: MetColors.successBg,
              selectedForegroundColor: MetColors.primary,
              side: const BorderSide(color: MetColors.borderStrong),
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            ),
          ),
        ),
        const SizedBox(height: 20),
        if (_tab == _ComplianceTab.alerts) const _AlertsSection() else const _RestrictiveListsSection(),
      ],
    );
  }
}

// ── Alertas ──────────────────────────────────────────────────────────────────

class _AlertsSection extends ConsumerWidget {
  const _AlertsSection();

  static const _statuses = ['OPEN', 'UNDER_REVIEW', 'DISMISSED', 'REPORTED'];
  static const _labels = {
    'OPEN': 'Abiertas',
    'UNDER_REVIEW': 'En revisión',
    'DISMISSED': 'Descartadas',
    'REPORTED': 'Reportadas',
  };

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final currentStatus = ref.watch(complianceAlertStatusFilterProvider);
    final alertsState = ref.watch(complianceAlertsProvider);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            Expanded(
              child: FilterBar<String>(
                selected: currentStatus,
                onChanged: (s) => ref.read(complianceAlertStatusFilterProvider.notifier).state = s,
                options: [for (final s in _statuses) FilterOption(s, _labels[s]!)],
              ),
            ),
            IconButton(
              tooltip: 'Actualizar',
              onPressed: () => ref.read(complianceAlertsProvider.notifier).load(),
              icon: const Icon(Icons.refresh_rounded),
            ),
          ],
        ),
        const SizedBox(height: 16),
        alertsState.when(
          loading: () => const LoadingState(label: 'Cargando alertas…'),
          error: (e, _) => ErrorState(error: e, onRetry: () => ref.read(complianceAlertsProvider.notifier).load()),
          data: (data) {
            final items = (data['items'] as List? ?? [])
                .map((e) => ComplianceAlert.fromJson(e as Map<String, dynamic>))
                .toList();
            final total = data['total'] as int? ?? items.length;
            if (items.isEmpty) {
              return Card(
                child: EmptyState(
                  icon: Icons.verified_user_outlined,
                  title: 'No hay alertas ${_labels[currentStatus]!.toLowerCase()}',
                  message: currentStatus == 'OPEN' ? 'Ninguna operación inusual espera revisión.' : null,
                ),
              );
            }
            return Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: Text(total == 1 ? '1 alerta' : '$total alertas',
                      style: const TextStyle(color: MetColors.textMuted, fontSize: 13.5, fontWeight: FontWeight.w600)),
                ),
                for (final alert in items)
                  Padding(padding: const EdgeInsets.only(bottom: 12), child: _AlertCard(alert: alert)),
              ],
            );
          },
        ),
      ],
    );
  }
}

class _AlertCard extends ConsumerWidget {
  const _AlertCard({required this.alert});

  final ComplianceAlert alert;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final severity = alertSeverity(alert.severity);
    final actionable = alert.status == 'OPEN' || alert.status == 'UNDER_REVIEW';

    return Card(
      clipBehavior: Clip.antiAlias,
      child: IntrinsicHeight(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(width: 4, color: severity.tone.foreground),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 18, 20, 18),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        StatusBadge(StatusInfo(alert.alertTypeLabel, severity.tone), showDot: false),
                        StatusBadge(severity),
                        if (alert.status != 'OPEN') StatusBadge(alertStatus(alert.status)),
                        Text(formatDateTime(alert.createdAt),
                            style: const TextStyle(fontSize: 13, color: MetColors.textMuted)),
                      ],
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        InitialsAvatar(alert.userFullName, size: 32),
                        const SizedBox(width: 10),
                        Flexible(
                          child: Text(alert.userFullName,
                              style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700)),
                        ),
                        const SizedBox(width: 8),
                        Text('· ${alert.userDocumentNumber}',
                            style: const TextStyle(fontSize: 14, color: MetColors.textMuted)),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Text(alert.description, style: const TextStyle(fontSize: 14.5, height: 1.5)),
                    if (alert.resolutionNotes != null && alert.resolutionNotes!.isNotEmpty) ...[
                      const SizedBox(height: 10),
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(color: MetColors.subtle, borderRadius: BorderRadius.circular(10)),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Icon(Icons.sticky_note_2_outlined, size: 18, color: MetColors.textMuted),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(alert.resolutionNotes!,
                                  style: const TextStyle(fontSize: 13.5, color: MetColors.textMuted)),
                            ),
                          ],
                        ),
                      ),
                    ],
                    if (actionable) ...[
                      const SizedBox(height: 14),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        alignment: WrapAlignment.end,
                        children: [
                          TextButton(
                            onPressed: () => _review(context, ref, 'DISMISSED'),
                            style: TextButton.styleFrom(foregroundColor: MetColors.textMuted),
                            child: const Text('Descartar'),
                          ),
                          if (alert.status == 'OPEN')
                            OutlinedButton(
                              onPressed: () => _review(context, ref, 'UNDER_REVIEW'),
                              child: const Text('Poner en revisión'),
                            ),
                          FilledButton.icon(
                            style: FilledButton.styleFrom(backgroundColor: MetColors.danger),
                            onPressed: () => _review(context, ref, 'REPORTED'),
                            icon: const Icon(Icons.flag_rounded, size: 18),
                            label: const Text('Marcar para ROS'),
                          ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _review(BuildContext context, WidgetRef ref, String newStatus) async {
    final notes = TextEditingController();
    final (title, description, confirmLabel, destructive) = switch (newStatus) {
      'DISMISSED' => ('Descartar alerta', 'La operación se considera normal y la alerta se cierra.', 'Descartar', false),
      'UNDER_REVIEW' => ('Poner en revisión', 'La alerta queda asignada para análisis.', 'Poner en revisión', false),
      _ => (
          'Marcar para ROS',
          'La alerta se marca para el Reporte de Operación Sospechosa ante la UIAF.',
          'Marcar para ROS',
          true
        ),
    };
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(title),
        content: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 440),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(description),
              const SizedBox(height: 6),
              Text('${alert.userFullName} · ${alert.alertTypeLabel}',
                  style: const TextStyle(fontWeight: FontWeight.w700, color: MetColors.text)),
              const SizedBox(height: 16),
              const FieldLabel('Notas', help: 'Opcional. Quedan en el historial de la alerta.'),
              TextField(
                controller: notes,
                maxLines: 3,
                autofocus: true,
                decoration: const InputDecoration(hintText: 'Ej.: Se validó el origen de los fondos con el socio'),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            style: TextButton.styleFrom(foregroundColor: MetColors.textMuted),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: destructive ? FilledButton.styleFrom(backgroundColor: MetColors.danger) : null,
            child: Text(confirmLabel),
          ),
        ],
      ),
    );
    if (confirmed != true || !context.mounted) return;

    try {
      await ref.read(complianceAlertsProvider.notifier).review(alert.id, newStatus, notes.text);
      if (context.mounted) showMetSnack(context, 'Alerta actualizada');
    } catch (e) {
      if (context.mounted) showErrorSnack(context, e);
    }
  }
}

// ── Listas restrictivas ─────────────────────────────────────────────────────

class _RestrictiveListsSection extends ConsumerStatefulWidget {
  const _RestrictiveListsSection();

  @override
  ConsumerState<_RestrictiveListsSection> createState() => _RestrictiveListsSectionState();
}

class _RestrictiveListsSectionState extends ConsumerState<_RestrictiveListsSection> {
  bool _refreshing = false;

  Future<void> _refreshLists() async {
    setState(() => _refreshing = true);
    try {
      await ref.read(restrictiveListMatchesProvider.notifier).refreshLists();
      if (mounted) showMetSnack(context, 'Listas OFAC y ONU actualizadas');
    } catch (e) {
      if (mounted) showErrorSnack(context, e);
    } finally {
      if (mounted) setState(() => _refreshing = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final matchesState = ref.watch(restrictiveListMatchesProvider);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionCard(
          padding: const EdgeInsets.all(20),
          child: LayoutBuilder(builder: (context, c) {
            final text = const Row(
              children: [
                IconBadge(Icons.public_rounded, tone: Tone.info),
                SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Screening automático', style: TextStyle(fontSize: 15, fontWeight: FontWeight.w700)),
                      SizedBox(height: 2),
                      Text('Cada socio se compara contra OFAC y la Lista Consolidada de la ONU. Las listas se actualizan solas cada noche.',
                          style: TextStyle(fontSize: 13.5, color: MetColors.textMuted, height: 1.45)),
                    ],
                  ),
                ),
              ],
            );
            final button = FilledButton.icon(
              onPressed: _refreshing ? null : _refreshLists,
              icon: _refreshing
                  ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Icon(Icons.cloud_download_rounded, size: 18),
              label: Text(_refreshing ? 'Actualizando…' : 'Actualizar listas ahora'),
            );
            if (c.maxWidth < 700) {
              return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [text, const SizedBox(height: 14), button]);
            }
            return Row(children: [Expanded(child: text), const SizedBox(width: 20), button]);
          }),
        ),
        const SizedBox(height: 16),
        matchesState.when(
          loading: () => const LoadingState(label: 'Cargando coincidencias…'),
          error: (e, _) => ErrorState(error: e, onRetry: () => ref.read(restrictiveListMatchesProvider.notifier).load()),
          data: (matches) {
            if (matches.isEmpty) {
              return const Card(
                child: EmptyState(
                  icon: Icons.verified_user_outlined,
                  title: 'Sin coincidencias',
                  message: 'Ningún socio aparece en las listas restrictivas.',
                ),
              );
            }
            return AdminTable<RestrictiveListMatch>(
              rows: matches,
              columns: [
                AdminColumn(
                  label: 'Socio',
                  flex: 4,
                  cell: (m) => TwoLineCell(m.userFullName, m.userDocumentNumber, leading: InitialsAvatar(m.userFullName)),
                ),
                AdminColumn(
                  label: 'Lista',
                  flex: 2,
                  cell: (m) => StatusBadge.of('Coincide en ${m.listType}', Tone.danger),
                ),
                AdminColumn(
                  label: 'Verificado',
                  flex: 2,
                  alignEnd: true,
                  cell: (m) => CellText(formatDateTime(m.checkedAt), muted: true),
                ),
              ],
            );
          },
        ),
      ],
    );
  }
}
