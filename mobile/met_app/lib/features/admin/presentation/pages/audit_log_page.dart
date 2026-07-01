import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/admin_provider.dart';
import '../../data/models/admin_models.dart';

class AuditLogPage extends ConsumerWidget {
  const AuditLogPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final logState = ref.watch(auditLogProvider);

    return Scaffold(
      backgroundColor: const Color(0xFF0D0D0D),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        foregroundColor: Colors.white,
        title: const Row(children: [
          Icon(Icons.history_rounded, color: Color(0xFF53A835), size: 24),
          SizedBox(width: 10),
          Expanded(child: Text('Log de Auditoría',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold))),
        ]),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded, color: Colors.white70),
            onPressed: () => ref.read(auditLogProvider.notifier).load(),
          ),
        ],
      ),
      body: logState.when(
        loading: () =>
            const Center(child: CircularProgressIndicator(color: Color(0xFF53A835))),
        error: (e, _) => Center(
            child: Text('Error: $e', style: const TextStyle(color: Colors.white70))),
        data: (data) {
          final entries = (data['data'] as List? ?? [])
              .map((e) => AuditLogEntry.fromJson(e as Map<String, dynamic>))
              .toList();
          final total = data['total'] as int? ?? 0;
          final page = data['page'] as int? ?? 0;

          return Column(
            children: [
              // Summary
              Container(
                margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
                decoration: BoxDecoration(
                  color: const Color(0xFF53A835).withOpacity(0.1),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: const Color(0xFF53A835).withOpacity(0.3)),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.format_list_bulleted_rounded,
                        color: Color(0xFF53A835), size: 20),
                    const SizedBox(width: 10),
                    Text('$total eventos registrados',
                        style: const TextStyle(
                            color: Colors.white, fontWeight: FontWeight.w600)),
                    const Spacer(),
                    Text('Pág. ${page + 1}',
                        style: TextStyle(color: Colors.white.withOpacity(0.5), fontSize: 13)),
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
                      color: Colors.white70,
                    ),
                    Text('Página ${page + 1}',
                        style: const TextStyle(color: Colors.white70)),
                    IconButton(
                      onPressed: entries.length == 20
                          ? () => ref.read(auditLogProvider.notifier).nextPage()
                          : null,
                      icon: const Icon(Icons.chevron_right_rounded),
                      color: Colors.white70,
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

  static Color _accionColor(String accion) {
    if (accion.contains('LOCK') || accion.contains('BLOCKED')) return const Color(0xFFCF3232);
    if (accion.contains('LOGIN')) return const Color(0xFF0B5FFF);
    if (accion.contains('CREATED')) return const Color(0xFF00A86B);
    if (accion.contains('UPDATED') || accion.contains('RESET')) return const Color(0xFF53A835);
    if (accion.contains('DELETED') || accion.contains('REVERSED')) return const Color(0xFFE91E63);
    return const Color(0xFF9E9E9E);
  }

  @override
  Widget build(BuildContext context) {
    final color = _accionColor(entry.accion);
    final ts = entry.timestamp.replaceAll('T', ' ').substring(0, 19);

    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.04),
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
                      color: Colors.white.withOpacity(0.4),
                      fontSize: 11, fontFamily: 'monospace')),
            ],
          ),
          if (entry.entidadAfectada != null) ...[
            const SizedBox(height: 8),
            Row(
              children: [
                Text('${entry.entidadAfectada}',
                    style: TextStyle(color: Colors.white.withOpacity(0.6), fontSize: 12)),
                if (entry.idEntidad != null)
                  Text(' → ${entry.idEntidad}',
                      style: TextStyle(
                          color: Colors.white.withOpacity(0.35),
                          fontSize: 11, fontFamily: 'monospace')),
              ],
            ),
          ],
          if (entry.motivo != null) ...[
            const SizedBox(height: 4),
            Text(entry.motivo!,
                style: TextStyle(color: Colors.white.withOpacity(0.8), fontSize: 12,
                    fontStyle: FontStyle.italic)),
          ],
          if (entry.ipOrigen != null) ...[
            const SizedBox(height: 4),
            Row(
              children: [
                Icon(Icons.location_on_rounded,
                    size: 12, color: Colors.white.withOpacity(0.3)),
                const SizedBox(width: 4),
                Text(entry.ipOrigen!,
                    style: TextStyle(
                        color: Colors.white.withOpacity(0.3),
                        fontSize: 11, fontFamily: 'monospace')),
              ],
            ),
          ],
        ],
      ),
    );
  }
}
