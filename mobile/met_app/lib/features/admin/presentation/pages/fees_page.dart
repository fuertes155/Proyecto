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
          ),
          IconButton(
            icon: Icon(Icons.refresh_rounded, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)),
            onPressed: () => ref.read(feeScheduleProvider.notifier).load(),
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
            itemBuilder: (context, i) => _FeeCard(fee: fees[i], color: primaryColor),
          );
        },
      ),
    );
  }

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
                  activeColor: color,
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
                foregroundColor: Colors.white,
              ),
              onPressed: () async {
                try {
                  await ref.read(feeScheduleProvider.notifier).createFee({
                    'tipoTarifa': tipoCtrl.text,
                    'descripcion': descCtrl.text,
                    'valor': double.parse(valCtrl.text),
                    'esPorcentaje': isPercent,
                  });
                  if (ctx.mounted) {
                    Navigator.of(ctx).pop();
                    ScaffoldMessenger.of(ctx).showSnackBar(SnackBar(
                      content: const Text('Tarifa creada'),
                      backgroundColor: color,
                    ));
                  }
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(SnackBar(
                      content: Text('Error: $e'),
                      backgroundColor: Theme.of(context).colorScheme.error,
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
  const _FeeCard({required this.fee, required this.color});
  final FeeSchedule fee;
  final Color color;

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
                      ? '${fee.valor}%'
                      : '\$${fee.valor.toStringAsFixed(0)}',
                  style: TextStyle(
                      color: color, fontWeight: FontWeight.bold, fontSize: 18)),
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
