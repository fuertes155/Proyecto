import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/admin_provider.dart';
import '../../data/models/admin_models.dart';

class FeesPage extends ConsumerWidget {
  const FeesPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final feesState = ref.watch(feeScheduleProvider);
    const primaryColor = Color(0xFF53A835);

    return Scaffold(
      backgroundColor: const Color(0xFF0D0D0D),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        foregroundColor: Colors.white,
        title: const Row(children: [
          Icon(Icons.percent_rounded, color: primaryColor, size: 24),
          SizedBox(width: 10),
          Expanded(child: Text('Tarifas y Comisiones',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold))),
        ]),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_rounded, color: Colors.white),
            onPressed: () => _showCreateDialog(context, ref, primaryColor),
          ),
          IconButton(
            icon: const Icon(Icons.refresh_rounded, color: Colors.white70),
            onPressed: () => ref.read(feeScheduleProvider.notifier).load(),
          ),
        ],
      ),
      body: feesState.when(
        loading: () =>
            const Center(child: CircularProgressIndicator(color: primaryColor)),
        error: (e, _) => Center(
            child: Text('Error: $e', style: const TextStyle(color: Colors.white70))),
        data: (fees) {
          if (fees.isEmpty) {
            return const Center(
                child: Text('No hay tarifas configuradas',
                    style: TextStyle(color: Colors.white54)));
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
          backgroundColor: const Color(0xFF1A1A1A),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          title: const Text('Nueva Tarifa',
              style: TextStyle(color: Colors.white, fontSize: 16)),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                _dialogField('Tipo (ej. TRANSFERENCIA)', tipoCtrl),
                const SizedBox(height: 10),
                _dialogField('Descripción (opcional)', descCtrl),
                const SizedBox(height: 10),
                _dialogField('Valor numérico', valCtrl, isNumber: true),
                const SizedBox(height: 10),
                SwitchListTile(
                  title: const Text('¿Es porcentaje?',
                      style: TextStyle(color: Colors.white)),
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
              child: const Text('Cancelar', style: TextStyle(color: Colors.white60)),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: color),
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
                    ScaffoldMessenger.of(ctx).showSnackBar(const SnackBar(
                      content: Text('Tarifa creada'),
                      backgroundColor: Color(0xFF00A86B),
                    ));
                  }
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(SnackBar(
                      content: Text('Error: $e'),
                      backgroundColor: const Color(0xFFCF3232),
                    ));
                  }
                }
              },
              child: const Text('Guardar', style: TextStyle(color: Colors.white)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _dialogField(String label, TextEditingController ctrl,
      {bool isNumber = false}) {
    return TextField(
      controller: ctrl,
      keyboardType: isNumber ? TextInputType.number : TextInputType.text,
      style: const TextStyle(color: Colors.white),
      decoration: InputDecoration(
        labelText: label,
        labelStyle: const TextStyle(color: Colors.white60, fontSize: 13),
        filled: true,
        fillColor: Colors.white.withOpacity(0.06),
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
    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.05),
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
                        style: const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                            fontSize: 16)),
                    if (fee.descripcion != null)
                      Text(fee.descripcion!,
                          style: TextStyle(
                              color: Colors.white.withOpacity(0.6), fontSize: 12)),
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
                  color: Colors.white.withOpacity(0.3), size: 14),
              const SizedBox(width: 6),
              Text('Desde: ${fee.vigentDesde.substring(0, 10)}',
                  style: TextStyle(
                      color: Colors.white.withOpacity(0.5), fontSize: 12)),
              if (fee.vigentaHasta != null) ...[
                const SizedBox(width: 12),
                Icon(Icons.event_busy_rounded,
                    color: Colors.white.withOpacity(0.3), size: 14),
                const SizedBox(width: 6),
                Text('Hasta: ${fee.vigentaHasta!.substring(0, 10)}',
                    style: TextStyle(
                        color: Colors.white.withOpacity(0.5), fontSize: 12)),
              ]
            ],
          )
        ],
      ),
    );
  }
}
