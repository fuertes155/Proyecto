import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/admin_provider.dart';
import '../../data/models/admin_models.dart';

/// Panel de cumplimiento SARLAFT: alertas de operaciones inusuales
/// (monto/frecuencia/patrón) y coincidencias contra listas restrictivas
/// (OFAC/ONU).
class CompliancePage extends ConsumerStatefulWidget {
  const CompliancePage({super.key});

  @override
  ConsumerState<CompliancePage> createState() => _CompliancePageState();
}

class _CompliancePageState extends ConsumerState<CompliancePage> with SingleTickerProviderStateMixin {
  late final TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Row(children: [
          Icon(Icons.shield_rounded, color: primaryColor, size: 24),
          const SizedBox(width: 10),
          const Expanded(child: Text('Cumplimiento SARLAFT',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold))),
        ]),
        bottom: TabBar(
          controller: _tabController,
          labelColor: primaryColor,
          unselectedLabelColor: Theme.of(context).colorScheme.onSurface.withOpacity(0.5),
          indicatorColor: primaryColor,
          tabs: const [
            Tab(text: 'Alertas'),
            Tab(text: 'Listas Restrictivas'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: const [
          _AlertsTab(),
          _RestrictiveListsTab(),
        ],
      ),
    );
  }
}

// ── Tab: Alertas ─────────────────────────────────────────────────────────────

class _AlertsTab extends ConsumerWidget {
  const _AlertsTab();

  static const _statuses = ['OPEN', 'UNDER_REVIEW', 'DISMISSED', 'REPORTED'];
  static const _statusLabels = {
    'OPEN': 'Abiertas',
    'UNDER_REVIEW': 'En revisión',
    'DISMISSED': 'Descartadas',
    'REPORTED': 'Reportadas',
  };

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final currentStatus = ref.watch(complianceAlertStatusFilterProvider);
    final alertsState = ref.watch(complianceAlertsProvider);
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: _statuses.map((s) {
                final selected = s == currentStatus;
                return Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(
                    label: Text(_statusLabels[s] ?? s),
                    selected: selected,
                    selectedColor: primaryColor.withOpacity(0.2),
                    onSelected: (_) => ref.read(complianceAlertStatusFilterProvider.notifier).state = s,
                  ),
                );
              }).toList(),
            ),
          ),
        ),
        Expanded(
          child: alertsState.when(
            loading: () => Center(child: CircularProgressIndicator(color: primaryColor)),
            error: (e, _) => Center(
                child: Text('Error: $e', style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)))),
            data: (data) {
              final items = (data['items'] as List? ?? [])
                  .map((e) => ComplianceAlert.fromJson(e as Map<String, dynamic>))
                  .toList();
              final total = data['total'] as int? ?? items.length;

              if (items.isEmpty) {
                return Center(
                  child: Text('No hay alertas ${(_statusLabels[currentStatus] ?? '').toLowerCase()}',
                      style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5))),
                );
              }

              return ListView.builder(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                itemCount: items.length + 1,
                itemBuilder: (context, i) {
                  if (i == 0) {
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: Text('$total alerta(s)',
                          style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5), fontSize: 13)),
                    );
                  }
                  return _AlertCard(alert: items[i - 1]);
                },
              );
            },
          ),
        ),
      ],
    );
  }
}

class _AlertCard extends ConsumerWidget {
  const _AlertCard({required this.alert});
  final ComplianceAlert alert;

  Color _severityColor() => switch (alert.severity) {
        'HIGH' => const Color(0xFFCF3232),
        'MEDIUM' => const Color(0xFFE8A33D),
        _ => const Color(0xFF0B5FFF),
      };

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final color = _severityColor();
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final ts = alert.createdAt.replaceAll('T', ' ').substring(0, 19);

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.04 : 0.02),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(color: color.withOpacity(0.2), borderRadius: BorderRadius.circular(6)),
                child: Text(alert.alertTypeLabel,
                    style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold)),
              ),
              const Spacer(),
              Text(ts,
                  style: TextStyle(
                      color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4), fontSize: 11, fontFamily: 'monospace')),
            ],
          ),
          const SizedBox(height: 10),
          Text('${alert.userFullName} · ${alert.userDocumentNumber}',
              style: TextStyle(color: Theme.of(context).colorScheme.onSurface, fontWeight: FontWeight.bold, fontSize: 14)),
          const SizedBox(height: 6),
          Text(alert.description,
              style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.75), fontSize: 13)),
          if (alert.status != 'OPEN') ...[
            const SizedBox(height: 8),
            Text('Estado: ${alert.status}${alert.resolutionNotes != null ? ' — ${alert.resolutionNotes}' : ''}',
                style: TextStyle(
                    color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5), fontSize: 12, fontStyle: FontStyle.italic)),
          ],
          if (alert.status == 'OPEN' || alert.status == 'UNDER_REVIEW') ...[
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton(
                  onPressed: () => _reviewDialog(context, ref, 'DISMISSED'),
                  child: const Text('Descartar'),
                ),
                const SizedBox(width: 8),
                TextButton(
                  onPressed: () => _reviewDialog(context, ref, 'UNDER_REVIEW'),
                  child: const Text('En revisión'),
                ),
                const SizedBox(width: 8),
                FilledButton(
                  style: FilledButton.styleFrom(backgroundColor: const Color(0xFFCF3232)),
                  onPressed: () => _reviewDialog(context, ref, 'REPORTED'),
                  child: const Text('Marcar para ROS'),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Future<void> _reviewDialog(BuildContext context, WidgetRef ref, String newStatus) async {
    final notesController = TextEditingController();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(_dialogTitle(newStatus)),
        content: TextField(
          controller: notesController,
          maxLines: 3,
          decoration: const InputDecoration(labelText: 'Notas (opcional)', border: OutlineInputBorder()),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(dialogContext, true), child: const Text('Confirmar')),
        ],
      ),
    );
    if (confirmed != true || !context.mounted) return;

    try {
      await ref.read(complianceAlertsProvider.notifier).review(alert.id, newStatus, notesController.text);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Alerta actualizada')));
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text('Error: $e'),
          backgroundColor: Theme.of(context).colorScheme.error,
        ));
      }
    }
  }

  String _dialogTitle(String status) => switch (status) {
        'DISMISSED' => 'Descartar alerta',
        'UNDER_REVIEW' => 'Marcar en revisión',
        'REPORTED' => 'Marcar para Reporte de Operación Sospechosa (ROS)',
        _ => 'Actualizar alerta',
      };
}

// ── Tab: Listas restrictivas ────────────────────────────────────────────────

class _RestrictiveListsTab extends ConsumerWidget {
  const _RestrictiveListsTab();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final matchesState = ref.watch(restrictiveListMatchesProvider);
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  'Screening contra OFAC y la Lista Consolidada de la ONU. Las listas se refrescan solas cada noche.',
                  style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6), fontSize: 12),
                ),
              ),
              const SizedBox(width: 8),
              FilledButton.icon(
                icon: const Icon(Icons.cloud_download_rounded, size: 18),
                label: const Text('Refrescar ahora'),
                onPressed: () async {
                  try {
                    final result = await ref.read(restrictiveListMatchesProvider.notifier).refreshLists();
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text('Listas refrescadas: $result')),
                      );
                    }
                  } catch (e) {
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                        content: Text('Error: $e'),
                        backgroundColor: Theme.of(context).colorScheme.error,
                      ));
                    }
                  }
                },
              ),
            ],
          ),
        ),
        Expanded(
          child: matchesState.when(
            loading: () => Center(child: CircularProgressIndicator(color: primaryColor)),
            error: (e, _) => Center(
                child: Text('Error: $e', style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)))),
            data: (matches) {
              if (matches.isEmpty) {
                return Center(
                  child: Text('Sin coincidencias en listas restrictivas',
                      style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5))),
                );
              }
              return ListView.builder(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                itemCount: matches.length,
                itemBuilder: (context, i) => _MatchCard(match: matches[i]),
              );
            },
          ),
        ),
      ],
    );
  }
}

class _MatchCard extends StatelessWidget {
  const _MatchCard({required this.match});
  final RestrictiveListMatch match;

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    const color = Color(0xFFCF3232);

    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.04 : 0.02),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: const BoxDecoration(shape: BoxShape.circle, color: Color(0x22CF3232)),
            child: const Icon(Icons.warning_amber_rounded, color: color, size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('${match.userFullName} · ${match.userDocumentNumber}',
                    style: TextStyle(color: Theme.of(context).colorScheme.onSurface, fontWeight: FontWeight.bold, fontSize: 14)),
                const SizedBox(height: 2),
                Text('Lista ${match.listType} · ${match.checkedAt.replaceAll('T', ' ').substring(0, 19)}',
                    style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5), fontSize: 12)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
