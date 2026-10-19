import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/ui/ui.dart';
import '../../data/models/admin_models.dart';
import '../providers/admin_provider.dart';
import '../shell/admin_nav.dart';

class MaintenancePage extends ConsumerWidget {
  const MaintenancePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(maintenanceProvider);
    final anyActive = state.valueOrNull?.any((w) => w.activo) ?? false;

    return AdminPage(
      section: sectionTitleFor(maintenanceItem.route),
      title: 'Mantenimiento',
      subtitle: 'Ventanas en las que la plataforma pausa operaciones para trabajos técnicos.',
      actions: [
        OutlinedButton.icon(
          onPressed: () => ref.read(maintenanceProvider.notifier).load(),
          icon: const Icon(Icons.refresh_rounded, size: 18),
          label: const Text('Actualizar'),
        ),
      ],
      children: [
        if (anyActive) ...[
          const CalloutBanner(
            tone: Tone.warning,
            title: 'Hay un mantenimiento activo',
            message: 'Las operaciones afectadas están pausadas para los socios hasta que lo desactives.',
          ),
          const SizedBox(height: 20),
        ],
        state.when(
          loading: () => const LoadingState(label: 'Cargando ventanas…'),
          error: (e, _) => ErrorState(error: e, onRetry: () => ref.read(maintenanceProvider.notifier).load()),
          data: (windows) {
            if (windows.isEmpty) {
              return const Card(
                child: EmptyState(
                  icon: Icons.build_circle_outlined,
                  title: 'No hay ventanas de mantenimiento',
                  message: 'Cuando se programe una aparecerá aquí para activarla.',
                ),
              );
            }
            return ResponsiveGrid(
              minItemWidth: 420,
              maxColumns: 2,
              children: [for (final w in windows) _MaintenanceCard(window: w)],
            );
          },
        ),
      ],
    );
  }
}

class _MaintenanceCard extends ConsumerStatefulWidget {
  const _MaintenanceCard({required this.window});

  final MaintenanceWindow window;

  @override
  ConsumerState<_MaintenanceCard> createState() => _MaintenanceCardState();
}

class _MaintenanceCardState extends ConsumerState<_MaintenanceCard> {
  bool _saving = false;

  String get _duration {
    final start = parseBackendDate(widget.window.inicio);
    final end = parseBackendDate(widget.window.fin);
    if (start == null || end == null || end.isBefore(start)) return '—';
    final d = end.difference(start);
    final h = d.inHours;
    final m = d.inMinutes % 60;
    if (h == 0) return '$m min';
    return m == 0 ? '$h h' : '$h h $m min';
  }

  Future<void> _toggle(bool activate) async {
    if (activate) {
      final confirmed = await confirmAction(
        context,
        title: 'Activar mantenimiento',
        message: 'Los socios verán la plataforma en mantenimiento y no podrán operar hasta que lo desactives.',
        confirmLabel: 'Activar ahora',
        destructive: true,
        icon: Icons.build_circle_outlined,
        details: [('Ventana', widget.window.descripcion), ('Duración prevista', _duration)],
      );
      if (!confirmed) return;
    }
    setState(() => _saving = true);
    try {
      await ref.read(maintenanceProvider.notifier).toggle(widget.window.id, activate);
      if (mounted) {
        showMetSnack(context, activate ? 'Mantenimiento activado' : 'Mantenimiento desactivado',
            tone: activate ? Tone.warning : Tone.success);
      }
    } catch (e) {
      if (mounted) showErrorSnack(context, e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final w = widget.window;
    return Card(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: w.activo ? MetColors.warning.withValues(alpha: 0.5) : MetColors.border),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                IconBadge(w.activo ? Icons.build_rounded : Icons.event_rounded, tone: w.activo ? Tone.warning : Tone.neutral),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(w.descripcion, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
                      const SizedBox(height: 4),
                      StatusBadge(w.activo ? const StatusInfo('En curso', Tone.warning) : const StatusInfo('Programada', Tone.neutral)),
                    ],
                  ),
                ),
                if (_saving)
                  const Padding(
                    padding: EdgeInsets.all(12),
                    child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)),
                  )
                else
                  Tooltip(
                    message: w.activo ? 'Desactivar mantenimiento' : 'Activar mantenimiento',
                    child: Switch(value: w.activo, onChanged: _toggle),
                  ),
              ],
            ),
            const SizedBox(height: 14),
            const Divider(),
            const SizedBox(height: 6),
            InfoRow('Inicio', formatDateTime(w.inicio), icon: Icons.play_circle_outline_rounded),
            InfoRow('Fin estimado', formatDateTime(w.fin), icon: Icons.stop_circle_outlined),
            InfoRow('Duración', _duration, icon: Icons.timelapse_rounded),
          ],
        ),
      ),
    );
  }
}
