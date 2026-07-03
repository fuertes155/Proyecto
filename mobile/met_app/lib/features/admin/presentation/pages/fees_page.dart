import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/admin_provider.dart';
import '../../data/models/admin_models.dart';

class FeesPage extends ConsumerWidget {
  const FeesPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final feesState = ref.watch(feeScheduleProvider);
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Row(children: [
          Icon(Icons.percent_rounded, color: primaryColor, size: 24),
          const SizedBox(width: 10),
          const Expanded(child: Text('Tarifas y Comisiones',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold))),
        ]),
        actions: [
          IconButton(
            icon: Icon(Icons.add_rounded, color: Theme.of(context).colorScheme.onSurface),
            onPressed: () => _showCreateDialog(context, ref, primaryColor),
            tooltip: 'Nueva tarifa',
          ),
          IconButton(
            icon: Icon(Icons.refresh_rounded, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)),
            onPressed: () => ref.read(feeScheduleProvider.notifier).load(),
            tooltip: 'Refrescar',
          ),
        ],
      ),
      body: feesState.when(
        loading: () =>
            Center(child: CircularProgressIndicator(color: primaryColor)),
        error: (e, _) => Center(
            child: Text('Error: $e', style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)))),
        data: (fees) {
          if (fees.isEmpty) {
            return Center(
                child: Text('No hay tarifas configuradas',
                    style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5))));
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: fees.length,
            itemBuilder: (context, i) => _FeeCard(
              fee: fees[i],
              color: primaryColor,
              onEdit: () => _showEditDialog(context, ref, fees[i], primaryColor),
              onDelete: () => _confirmDelete(context, ref, fees[i], primaryColor),
            ),
          );
        },
      ),
    );
  }

  // ── Crear ──────────────────────────────────────────────────────────────────
  void _showCreateDialog(BuildContext context, WidgetRef ref, Color color) {
    final tipoCtrl = TextEditingController();
    final descCtrl = TextEditingController();
    final valCtrl = TextEditingController();
    bool isPercent = true;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setState) => AlertDialog(
          backgroundColor: Theme.of(context).colorScheme.surface,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          title: Text('Nueva Tarifa',
              style: TextStyle(color: Theme.of(context).colorScheme.onSurface, fontSize: 16)),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                _dialogField('Tipo (ej. TRANSFERENCIA)', tipoCtrl, context),
                const SizedBox(height: 10),
                _dialogField('Descripción (opcional)', descCtrl, context),
                const SizedBox(height: 10),
                _dialogField('Valor numérico', valCtrl, context, isNumber: true),
                const SizedBox(height: 10),
                SwitchListTile(
                  title: Text('¿Es porcentaje?',
                      style: TextStyle(color: Theme.of(context).colorScheme.onSurface)),
                  activeThumbColor: color,
                  value: isPercent,
                  onChanged: (v) => setState(() => isPercent = v),
                )
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: Text('Cancelar', style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6))),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: color,
                foregroundColor: Theme.of(context).colorScheme.onSurface,
              ),
              onPressed: () async {
                try {
                  await ref.read(feeScheduleProvider.notifier).createFee({
                    'tipoTarifa': tipoCtrl.text,
                    'descripcion': descCtrl.text,
                    'valor': double.parse(valCtrl.text),
                    'esPorcentaje': isPercent,
                    'vigentDesde': DateTime.now().toUtc().toIso8601String(),
                  });
                  if (ctx.mounted) {
                    Navigator.of(ctx).pop();
                    ScaffoldMessenger.of(ctx).showSnackBar(SnackBar(
                      content: const Text('✓ Tarifa creada'),
                      backgroundColor: color,
                      behavior: SnackBarBehavior.floating,
                    ));
                  }
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(SnackBar(
                      content: Text('Error: $e'),
                      backgroundColor: Theme.of(context).colorScheme.error,
                      behavior: SnackBarBehavior.floating,
                    ));
                  }
                }
              },
              child: const Text('Guardar'),
            ),
          ],
        ),
      ),
    );
  }

  // ── Editar ─────────────────────────────────────────────────────────────────
  void _showEditDialog(BuildContext context, WidgetRef ref, FeeSchedule fee, Color color) {
    final valCtrl = TextEditingController(text: fee.valor.toString());
    final descCtrl = TextEditingController(text: fee.descripcion ?? '');
    bool isPercent = fee.esPorcentaje;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setState) => AlertDialog(
          backgroundColor: Theme.of(context).colorScheme.surface,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          title: Row(
            children: [
              Icon(Icons.edit_rounded, color: color, size: 20),
              const SizedBox(width: 8),
              Expanded(
                child: Text('Editar: ${fee.tipoTarifa}',
                    style: TextStyle(
                        color: Theme.of(context).colorScheme.onSurface, fontSize: 15)),
              ),
            ],
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                _dialogField('Descripción', descCtrl, context),
                const SizedBox(height: 10),
                _dialogField('Nuevo valor numérico', valCtrl, context, isNumber: true),
                const SizedBox(height: 10),
                SwitchListTile(
                  title: Text('¿Es porcentaje?',
                      style: TextStyle(color: Theme.of(context).colorScheme.onSurface)),
                  activeThumbColor: color,
                  value: isPercent,
                  onChanged: (v) => setState(() => isPercent = v),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: Text('Cancelar',
                  style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6))),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: color,
                foregroundColor: Theme.of(context).colorScheme.onSurface,
              ),
              onPressed: () async {
                try {
                  await ref.read(feeScheduleProvider.notifier).updateFee(fee.id, {
                    'tipoTarifa': fee.tipoTarifa,
                    'descripcion': descCtrl.text,
                    'valor': double.parse(valCtrl.text),
                    'esPorcentaje': isPercent,
                    'vigentDesde': DateTime.now().toUtc().toIso8601String(),
                  });
                  if (ctx.mounted) {
                    Navigator.of(ctx).pop();
                    ScaffoldMessenger.of(ctx).showSnackBar(SnackBar(
                      content: const Text('✓ Tarifa actualizada'),
                      backgroundColor: color,
                      behavior: SnackBarBehavior.floating,
                    ));
                  }
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(SnackBar(
                      content: Text('Error: $e'),
                      backgroundColor: Theme.of(context).colorScheme.error,
                      behavior: SnackBarBehavior.floating,
                    ));
                  }
                }
              },
              child: const Text('Actualizar'),
            ),
          ],
        ),
      ),
    );
  }

  // ── Eliminar ───────────────────────────────────────────────────────────────
  void _confirmDelete(BuildContext context, WidgetRef ref, FeeSchedule fee, Color color) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: Theme.of(context).colorScheme.surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Row(
          children: [
            Icon(Icons.warning_amber_rounded, color: Theme.of(context).colorScheme.error, size: 22),
            const SizedBox(width: 8),
            const Text('Eliminar tarifa', style: TextStyle(fontSize: 15)),
          ],
        ),
        content: Text(
          '¿Estás seguro de que deseas eliminar la tarifa "${fee.tipoTarifa}"?\n\nEsta acción queda registrada en el log de auditoría.',
          style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.75), fontSize: 13),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: Text('Cancelar',
                style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6))),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: Theme.of(context).colorScheme.error,
              foregroundColor: Colors.white,
            ),
            onPressed: () async {
              Navigator.of(ctx).pop();
              try {
                await ref.read(feeScheduleProvider.notifier).deleteFee(fee.id);
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
                    content: Text('✓ Tarifa eliminada'),
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
            child: const Text('Eliminar'),
          ),
        ],
      ),
    );
  }

  Widget _dialogField(String label, TextEditingController ctrl, BuildContext context,
      {bool isNumber = false}) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return TextField(
      controller: ctrl,
      keyboardType: isNumber ? TextInputType.number : TextInputType.text,
      style: TextStyle(color: Theme.of(context).colorScheme.onSurface),
      decoration: InputDecoration(
        labelText: label,
        labelStyle: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6), fontSize: 13),
        filled: true,
        fillColor: Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.06 : 0.03),
        border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
      ),
    );
  }
}

class _FeeCard extends StatelessWidget {
  const _FeeCard({
    required this.fee,
    required this.color,
    required this.onEdit,
    required this.onDelete,
  });
  final FeeSchedule fee;
  final Color color;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.05 : 0.03),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                    shape: BoxShape.circle, color: color.withOpacity(0.2)),
                child: Icon(Icons.percent_rounded, color: color, size: 22),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(fee.tipoTarifa,
                        style: TextStyle(
                            color: Theme.of(context).colorScheme.onSurface,
                            fontWeight: FontWeight.bold,
                            fontSize: 16)),
                    if (fee.descripcion != null)
                      Text(fee.descripcion!,
                          style: TextStyle(
                              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6), fontSize: 12)),
                  ],
                ),
              ),
              Text(
                  fee.esPorcentaje
                      ? '${(fee.valor * 100).toStringAsFixed(2)}%'
                      : '\$${fee.valor.toStringAsFixed(0)}',
                  style: TextStyle(
                      color: color, fontWeight: FontWeight.bold, fontSize: 18)),
              const SizedBox(width: 4),
              // Botón editar
              IconButton(
                icon: Icon(Icons.edit_rounded,
                    color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5), size: 18),
                onPressed: onEdit,
                tooltip: 'Editar tarifa',
                constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
                padding: EdgeInsets.zero,
              ),
              // Botón eliminar
              IconButton(
                icon: Icon(Icons.delete_outline_rounded,
                    color: Theme.of(context).colorScheme.error.withOpacity(0.7), size: 18),
                onPressed: onDelete,
                tooltip: 'Eliminar tarifa',
                constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
                padding: EdgeInsets.zero,
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Icon(Icons.calendar_today_rounded,
                  color: Theme.of(context).colorScheme.onSurface.withOpacity(0.3), size: 14),
              const SizedBox(width: 6),
              Text('Desde: ${fee.vigentDesde.substring(0, 10)}',
                  style: TextStyle(
                      color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5), fontSize: 12)),
              if (fee.vigentaHasta != null) ...[
                const SizedBox(width: 12),
                Icon(Icons.event_busy_rounded,
                    color: Theme.of(context).colorScheme.onSurface.withOpacity(0.3), size: 14),
                const SizedBox(width: 6),
                Text('Hasta: ${fee.vigentaHasta!.substring(0, 10)}',
                    style: TextStyle(
                        color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5), fontSize: 12)),
              ]
            ],
          )
        ],
      ),
    );
  }
}
