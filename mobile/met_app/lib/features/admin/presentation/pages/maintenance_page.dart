import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/admin_provider.dart';
import '../../data/models/admin_models.dart';

class MaintenancePage extends ConsumerWidget {
  const MaintenancePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(maintenanceProvider);
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Row(children: [
          Icon(Icons.build_circle_rounded, color: primaryColor, size: 24),
          const SizedBox(width: 10),
          const Expanded(child: Text('Modo Mantenimiento',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold))),
        ]),
        actions: [
          IconButton(
            icon: Icon(Icons.refresh_rounded, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)),
            onPressed: () => ref.read(maintenanceProvider.notifier).load(),
          ),
        ],
      ),
      body: state.when(
        loading: () =>
            Center(child: CircularProgressIndicator(color: primaryColor)),
        error: (e, _) => Center(
            child: Text('Error: $e', style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)))),
        data: (windows) {
          if (windows.isEmpty) {
            return Center(
              child: Text('No hay ventanas de mantenimiento configuradas.',
                  style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5))),
            );
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: windows.length,
            itemBuilder: (context, i) =>
                _MaintenanceCard(window: windows[i], color: primaryColor),
          );
        },
      ),
    );
  }
}

class _MaintenanceCard extends ConsumerWidget {
  const _MaintenanceCard({required this.window, required this.color});
  final MaintenanceWindow window;
  final Color color;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.05 : 0.03),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
            color: window.activo ? color.withOpacity(0.5) : Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.12 : 0.05)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: window.activo
                        ? color.withOpacity(0.2)
                        : Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.05 : 0.03)),
                child: Icon(
                    window.activo ? Icons.build_rounded : Icons.power_off_rounded,
                    color: window.activo ? color : Theme.of(context).colorScheme.onSurface.withOpacity(0.4),
                    size: 22),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(window.descripcion,
                    style: TextStyle(
                        color: Theme.of(context).colorScheme.onSurface,
                        fontWeight: FontWeight.bold,
                        fontSize: 16)),
              ),
              Switch(
                value: window.activo,
                activeThumbColor: color,
                onChanged: (val) async {
                  try {
                    await ref
                        .read(maintenanceProvider.notifier)
                        .toggle(window.id, val);
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                        content: Text(val
                            ? 'Mantenimiento activado'
                            : 'Mantenimiento desactivado'),
                        backgroundColor: val ? color : Colors.grey[800],
                        behavior: SnackBarBehavior.floating,
                      ));
                    }
                  } catch (e) {
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                        content: Text('Error: $e'),
                        backgroundColor: Theme.of(context).colorScheme.error,
                        behavior: SnackBarBehavior.floating,
                      ));
                    }
                  }
                },
              ),
            ],
          ),
          const SizedBox(height: 14),
          _DetailRow('Inicio', window.inicio),
          const SizedBox(height: 6),
          _DetailRow('Fin estimado', window.fin),
        ],
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  const _DetailRow(this.label, this.value);
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(Icons.access_time_rounded, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4), size: 16),
        const SizedBox(width: 8),
        Text(label,
            style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5), fontSize: 13)),
        const SizedBox(width: 8),
        Expanded(
          child: Text(value.replaceAll('T', ' ').length > 19 ? value.replaceAll('T', ' ').substring(0, 19) : value,
              textAlign: TextAlign.right,
              style: TextStyle(
                  color: Theme.of(context).colorScheme.onSurface, fontWeight: FontWeight.w500, fontSize: 13)),
        ),
      ],
    );
  }
}
