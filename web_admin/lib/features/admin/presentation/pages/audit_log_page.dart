import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/ui/ui.dart';
import '../../data/models/admin_models.dart';
import '../providers/admin_provider.dart';
import '../shell/admin_nav.dart';

const _pageSize = 20;

// Códigos que registra el backend (AdminAuditEntry.accion / entidadAfectada).
const _knownActions = {
  'ADMIN_LOGIN': 'Inicio de sesión',
  'ADMIN_SUSPENDED': 'Administrador suspendido',
  'COMPLIANCE_ALERT_REVIEWED': 'Alerta SARLAFT revisada',
  'EMERGENCY_LOCK_USER': 'Bloqueo de usuario',
  'EMERGENCY_REVOKE_TOKENS': 'Sesiones revocadas',
  'FEE_SCHEDULE_CREATED': 'Tarifa creada',
  'FEE_SCHEDULE_UPDATED': 'Tarifa actualizada',
  'FEE_SCHEDULE_DELETED': 'Tarifa eliminada',
  'MAINTENANCE_SCHEDULED': 'Mantenimiento programado',
  'MAINTENANCE_ACTIVATED': 'Mantenimiento activado',
  'MAINTENANCE_DEACTIVATED': 'Mantenimiento desactivado',
  'OPERATION_LIMIT_UPDATED': 'Límite actualizado',
  'RESTRICTIVE_LIST_REFRESH_TRIGGERED': 'Listas restrictivas actualizadas',
  'RISK_RULE_CREATED': 'Regla de riesgo creada',
  'RISK_RULE_ENABLED': 'Regla de riesgo activada',
  'RISK_RULE_DISABLED': 'Regla de riesgo pausada',
  'RISK_RULE_DELETED': 'Regla de riesgo eliminada',
  'TRANSACTION_REVERSED': 'Reversión registrada',
  'USER_CREDENTIALS_RESET': 'Credenciales reseteadas',
};

const _knownEntities = {
  'ADMIN': 'Administrador',
  'USER': 'Usuario',
  'KYC': 'KYC',
  'TRANSACTION': 'Transacción',
  'FEE_SCHEDULE': 'Tarifa',
  'RISK_RULE': 'Regla de riesgo',
  'MAINTENANCE_WINDOW': 'Mantenimiento',
  'COMPLIANCE_ALERT': 'Alerta SARLAFT',
};

/// "FEE_CREATED" → "Fee created" cuando no hay traducción conocida.
String _actionLabel(String code) {
  final known = _knownActions[code];
  if (known != null) return known;
  final text = code.toLowerCase().replaceAll('_', ' ');
  return text.isEmpty ? code : text[0].toUpperCase() + text.substring(1);
}

Tone _actionTone(String action) {
  if (action.contains('LOCK') || action.contains('BLOCK') || action.contains('FAILED') || action.contains('SUSPENDED')) {
    return Tone.danger;
  }
  if (action.contains('REVERS') ||
      action.contains('DELETE') ||
      action.contains('RESET') ||
      action.contains('REVOKE') ||
      action.contains('DISABLED') ||
      action.contains('ACTIVATED')) {
    return Tone.warning;
  }
  if (action.contains('LOGIN')) return Tone.info;
  if (action.contains('CREATE') || action.contains('ENABLED') || action.contains('REVIEWED')) return Tone.success;
  return Tone.neutral;
}

class AuditLogPage extends ConsumerWidget {
  const AuditLogPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final logState = ref.watch(auditLogProvider);

    return AdminPage(
      section: sectionTitleFor(auditLogItem.route),
      title: 'Auditoría',
      subtitle: 'Cada acción administrativa queda registrada con quién, cuándo, desde dónde y por qué.',
      actions: [
        OutlinedButton.icon(
          onPressed: () => ref.read(auditLogProvider.notifier).load(),
          icon: const Icon(Icons.refresh_rounded, size: 18),
          label: const Text('Actualizar'),
        ),
      ],
      children: [
        logState.when(
          loading: () => const LoadingState(label: 'Cargando registros…'),
          error: (e, _) => ErrorState(error: e, onRetry: () => ref.read(auditLogProvider.notifier).load()),
          data: (data) {
            final entries = (data['data'] as List? ?? [])
                .map((e) => AuditLogEntry.fromJson(e as Map<String, dynamic>))
                .toList();
            final total = data['total'] as int? ?? entries.length;
            final page = data['page'] as int? ?? 0;

            if (entries.isEmpty && page == 0) {
              return const Card(
                child: EmptyState(icon: Icons.history_rounded, title: 'Todavía no hay registros de auditoría'),
              );
            }

            final from = page * _pageSize + 1;
            final to = page * _pageSize + entries.length;
            final hasNext = entries.length == _pageSize && to < total;

            return AdminTable<AuditLogEntry>(
              rows: entries,
              columns: [
                AdminColumn(
                  label: 'Fecha',
                  flex: 2,
                  cell: (e) => CellText(formatDateTime(e.timestamp)),
                ),
                AdminColumn(
                  label: 'Acción',
                  flex: 3,
                  cell: (e) => StatusBadge(StatusInfo(_actionLabel(e.accion), _actionTone(e.accion))),
                ),
                AdminColumn(
                  label: 'Sobre',
                  flex: 3,
                  cell: (e) => TwoLineCell(
                    _knownEntities[e.entidadAfectada] ?? e.entidadAfectada ?? '—',
                    e.idEntidad,
                    monospaceSecondary: true,
                  ),
                ),
                AdminColumn(
                  label: 'Motivo',
                  flex: 4,
                  cell: (e) => CellText(e.motivo ?? '—', muted: e.motivo == null),
                ),
                AdminColumn(
                  label: 'IP',
                  flex: 2,
                  alignEnd: true,
                  cell: (e) => Text(e.ipOrigen ?? '—',
                      style: const TextStyle(fontFamily: 'monospace', fontSize: 13, color: MetColors.textMuted)),
                ),
              ],
              footer: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                child: Row(
                  children: [
                    Expanded(
                      child: Text('Mostrando $from–$to de $total',
                          style: const TextStyle(fontSize: 13.5, color: MetColors.textMuted)),
                    ),
                    OutlinedButton.icon(
                      onPressed: page > 0 ? () => ref.read(auditLogProvider.notifier).prevPage() : null,
                      icon: const Icon(Icons.chevron_left_rounded, size: 20),
                      label: const Text('Anterior'),
                    ),
                    const SizedBox(width: 8),
                    OutlinedButton(
                      onPressed: hasNext ? () => ref.read(auditLogProvider.notifier).nextPage() : null,
                      child: const Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [Text('Siguiente'), SizedBox(width: 4), Icon(Icons.chevron_right_rounded, size: 20)],
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        ),
      ],
    );
  }
}
