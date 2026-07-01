import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/admin_provider.dart';
import '../../data/models/admin_models.dart';

class OperationLimitsPage extends ConsumerWidget {
  const OperationLimitsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final limitsState = ref.watch(operationLimitsProvider);

    return Scaffold(
      backgroundColor: const Color(0xFF0D0D0D),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        foregroundColor: Colors.white,
        title: const Row(children: [
          Icon(Icons.attach_money_rounded, color: Color(0xFF53A835), size: 24),
          SizedBox(width: 10),
          Expanded(child: Text('Límites de Operación',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold))),
        ]),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded, color: Colors.white70),
            onPressed: () => ref.read(operationLimitsProvider.notifier).load(),
          ),
        ],
      ),
      body: limitsState.when(
        loading: () => const Center(child: CircularProgressIndicator(color: Color(0xFF53A835))),
        error: (e, _) => Center(child: Text('Error: $e', style: const TextStyle(color: Colors.white70))),
        data: (limits) => ListView.builder(
          padding: const EdgeInsets.all(16),
          itemCount: limits.length,
          itemBuilder: (context, i) => _LimitCard(
            limit: limits[i],
            onEdit: () => _showEditDialog(context, ref, limits[i]),
          ),
        ),
      ),
    );
  }

  void _showEditDialog(BuildContext context, WidgetRef ref, OperationLimit limit) {
    final diarCtrl = TextEditingController(text: limit.montoDiarioMax.toString());
    final txCtrl = TextEditingController(text: limit.montoPorTransaccionMax.toString());

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1A1A1A),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text('Editar: ${limit.tipoOperacion}',
            style: const TextStyle(color: Colors.white, fontSize: 16)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _dialogField('Monto diario máximo (COP)', diarCtrl, Icons.today_rounded),
            const SizedBox(height: 14),
            _dialogField('Monto por transacción máximo', txCtrl, Icons.receipt_long_rounded),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Cancelar', style: TextStyle(color: Colors.white60)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF53A835)),
            onPressed: () async {
              Navigator.of(ctx).pop();
              try {
                await ref.read(operationLimitsProvider.notifier).update(
                      limit.tipoOperacion,
                      int.parse(diarCtrl.text),
                      int.parse(txCtrl.text),
                    );
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
                    content: Text('✓ Límites actualizados'),
                    backgroundColor: Color(0xFF53A835),
                    behavior: SnackBarBehavior.floating,
                  ));
                }
              } catch (e) {
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                    content: Text('Error: $e'),
                    backgroundColor: const Color(0xFFCF3232),
                    behavior: SnackBarBehavior.floating,
                  ));
                }
              }
            },
            child: const Text('Guardar', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
    diarCtrl.dispose();
    txCtrl.dispose();
  }

  Widget _dialogField(String label, TextEditingController ctrl, IconData icon) {
    return TextField(
      controller: ctrl,
      keyboardType: TextInputType.number,
      style: const TextStyle(color: Colors.white),
      decoration: InputDecoration(
        labelText: label,
        labelStyle: const TextStyle(color: Colors.white60, fontSize: 13),
        prefixIcon: Icon(icon, color: Colors.white38, size: 20),
        filled: true,
        fillColor: Colors.white.withOpacity(0.06),
        border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
        enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide(color: Colors.white.withOpacity(0.12))),
        focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: Color(0xFF53A835), width: 1.5)),
      ),
    );
  }
}

class _LimitCard extends StatelessWidget {
  const _LimitCard({required this.limit, required this.onEdit});
  final OperationLimit limit;
  final VoidCallback onEdit;

  static const _typeColors = {
    'TRANSFER': Color(0xFF0B5FFF),
    'WITHDRAWAL': Color(0xFFFF6B00),
    'PURCHASE': Color(0xFF9C27B0),
  };

  static const _typeIcons = {
    'TRANSFER': Icons.swap_horiz_rounded,
    'WITHDRAWAL': Icons.money_off_rounded,
    'PURCHASE': Icons.shopping_cart_rounded,
  };

  String _formatCOP(int amount) {
    final str = amount.toString();
    final buffer = StringBuffer();
    for (int i = 0; i < str.length; i++) {
      if (i > 0 && (str.length - i) % 3 == 0) buffer.write('.');
      buffer.write(str[i]);
    }
    return '\$ ${buffer.toString()}';
  }

  @override
  Widget build(BuildContext context) {
    final color = _typeColors[limit.tipoOperacion] ?? Colors.white;
    final icon = _typeIcons[limit.tipoOperacion] ?? Icons.money_rounded;

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
                    shape: BoxShape.circle, color: color.withOpacity(0.15)),
                child: Icon(icon, color: color, size: 22),
              ),
              const SizedBox(width: 12),
              Text(limit.tipoOperacion,
                  style: const TextStyle(
                      color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
              const Spacer(),
              IconButton(
                icon: const Icon(Icons.edit_rounded, color: Colors.white54, size: 20),
                onPressed: onEdit,
                tooltip: 'Editar límites',
              ),
            ],
          ),
          const SizedBox(height: 14),
          _LimitRow('Diario máximo', _formatCOP(limit.montoDiarioMax), Icons.today_rounded),
          const SizedBox(height: 6),
          _LimitRow('Por transacción', _formatCOP(limit.montoPorTransaccionMax),
              Icons.receipt_long_rounded),
        ],
      ),
    );
  }
}

class _LimitRow extends StatelessWidget {
  const _LimitRow(this.label, this.value, this.icon);
  final String label;
  final String value;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, color: Colors.white38, size: 16),
        const SizedBox(width: 8),
        Text(label, style: TextStyle(color: Colors.white.withOpacity(0.5), fontSize: 13)),
        const Spacer(),
        Text(value,
            style: const TextStyle(
                color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
      ],
    );
  }
}
