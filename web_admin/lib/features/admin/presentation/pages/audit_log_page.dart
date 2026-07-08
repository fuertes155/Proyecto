import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/admin_provider.dart';
import '../../data/models/admin_models.dart';

class AuditLogPage extends ConsumerWidget {
  const AuditLogPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final logState = ref.watch(auditLogProvider);
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Row(children: [
          Icon(Icons.history_rounded, color: primaryColor, size: 24),
          const SizedBox(width: 10),
          const Expanded(child: Text('Log de Auditoría',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold))),
        ]),
        actions: [
          IconButton(
            icon: Icon(Icons.refresh_rounded, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)),
            onPressed: () => ref.read(auditLogProvider.notifier).load(),
          ),
        ],
      ),
      body: logState.when(
        loading: () =>
            Center(child: CircularProgressIndicator(color: primaryColor)),
        error: (e, _) => Center(
            child: Text('Error: $e', style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)))),
        data: (data) {
          final entries = (data['data'] as List? ?? [])
              .map((e) => AuditLogEntry.fromJson(e as Map<String, dynamic>))
              .toList();
          final total = data['total'] as int? ?? 0;
          final page = data['page'] as int? ?? 0;
          final isDark = Theme.of(context).brightness == Brightness.dark;

          return Column(
            children: [
              // Summary
              Container(
                margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
                decoration: BoxDecoration(
                  color: primaryColor.withOpacity(isDark ? 0.1 : 0.05),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: primaryColor.withOpacity(0.3)),
                ),
                child: Row(
                  children: [
                    Icon(Icons.format_list_bulleted_rounded,
                        color: primaryColor, size: 20),
                    const SizedBox(width: 10),
                    Text('$total eventos registrados',
                        style: TextStyle(
                            color: Theme.of(context).colorScheme.onSurface, fontWeight: FontWeight.w600)),
                    const Spacer(),
                    Text('Pág. ${page + 1}',
                        style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5), fontSize: 13)),
                  ],
                ),
              ),
              Expanded(
                child: ListView.builder(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  itemCount: entries.length,
                  itemBuilder: (context, i) => _AuditCard(entry: entries[i]),
                ),
              ),
              // Paginación
              Padding(
                padding: const EdgeInsets.all(12),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    IconButton(
                      onPressed: page > 0
                          ? () => ref.read(auditLogProvider.notifier).prevPage()
                          : null,
                      icon: const Icon(Icons.chevron_left_rounded),
                      color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
                    ),
                    Text('Página ${page + 1}',
                        style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7))),
                    IconButton(
                      onPressed: entries.length == 20
                          ? () => ref.read(auditLogProvider.notifier).nextPage()
                          : null,
                      icon: const Icon(Icons.chevron_right_rounded),
                      color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
                    ),
                  ],
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _AuditCard extends StatelessWidget {
  const _AuditCard({required this.entry});
  final AuditLogEntry entry;

  static Color _accionColor(String accion, BuildContext context) {
    if (accion.contains('LOCK') || accion.contains('BLOCKED')) return const Color(0xFFCF3232);
    if (accion.contains('LOGIN')) return const Color(0xFF0B5FFF);
    if (accion.contains('CREATED')) return const Color(0xFF00A86B);
    if (accion.contains('UPDATED') || accion.contains('RESET')) return Theme.of(context).colorScheme.primary;
    if (accion.contains('DELETED') || accion.contains('REVERSED')) return const Color(0xFFE91E63);
    return const Color(0xFF9E9E9E);
  }

  @override
  Widget build(BuildContext context) {
    final color = _accionColor(entry.accion, context);
    final ts = entry.timestamp.replaceAll('T', ' ').substring(0, 19);
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.04 : 0.02),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: color.withOpacity(0.25)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: color.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(entry.accion,
                    style: TextStyle(
                        color: color, fontSize: 11, fontWeight: FontWeight.bold,
                        letterSpacing: 0.5)),
              ),
              const Spacer(),
              Text(ts,
                  style: TextStyle(
                      color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4),
                      fontSize: 11, fontFamily: 'monospace')),
            ],
          ),
          if (entry.entidadAfectada != null) ...[
            const SizedBox(height: 8),
            Row(
              children: [
                Text('${entry.entidadAfectada}',
                    style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6), fontSize: 12)),
                if (entry.idEntidad != null)
                  Text(' → ${entry.idEntidad}',
                      style: TextStyle(
                          color: Theme.of(context).colorScheme.onSurface.withOpacity(0.35),
                          fontSize: 11, fontFamily: 'monospace')),
              ],
            ),
          ],
          if (entry.motivo != null) ...[
            const SizedBox(height: 4),
            Text(entry.motivo!,
                style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.8), fontSize: 12,
                    fontStyle: FontStyle.italic)),
          ],
          if (entry.ipOrigen != null) ...[
            const SizedBox(height: 4),
            Row(
              children: [
                Icon(Icons.location_on_rounded,
                    size: 12, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.3)),
                const SizedBox(width: 4),
                Text(entry.ipOrigen!,
                    style: TextStyle(
                        color: Theme.of(context).colorScheme.onSurface.withOpacity(0.3),
                        fontSize: 11, fontFamily: 'monospace')),
              ],
            ),
          ],
        ],
      ),
    );
  }
}
