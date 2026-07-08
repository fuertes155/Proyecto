import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/admin_provider.dart';
import '../../data/models/admin_models.dart';

class OperationLimitsPage extends ConsumerWidget {
  const OperationLimitsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final limitsState = ref.watch(operationLimitsProvider);
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Row(children: [
          Icon(Icons.attach_money_rounded, color: primaryColor, size: 24),
          const SizedBox(width: 10),
          const Expanded(child: Text('Límites de Operación',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold))),
        ]),
        actions: [
          IconButton(
            icon: Icon(Icons.refresh_rounded, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)),
            onPressed: () => ref.read(operationLimitsProvider.notifier).load(),
          ),
        ],
      ),
      body: limitsState.when(
        loading: () => Center(child: CircularProgressIndicator(color: primaryColor)),
        error: (e, _) => Center(child: Text('Error: $e', style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)))),
        data: (limits) {
          // Excluir PURCHASE y deduplicar por tipoOperacion
          final seen = <String>{};
          final filtered = limits
              .where((l) => l.tipoOperacion != 'PURCHASE')
              .where((l) => seen.add(l.tipoOperacion))
              .toList();
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: filtered.length,
            itemBuilder: (context, i) => _LimitCard(
              limit: filtered[i],
              onEdit: () => _showEditDialog(context, ref, filtered[i]),
            ),
          );
        },
      ),
    );
  }

  void _showEditDialog(BuildContext context, WidgetRef ref, OperationLimit limit) {
    final diarCtrl = TextEditingController(text: limit.montoDiarioMax.toString());
    final txCtrl = TextEditingController(text: limit.montoPorTransaccionMax.toString());
    final primaryColor = Theme.of(context).colorScheme.primary;

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: Theme.of(context).colorScheme.surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text('Editar: ${limit.tipoOperacion}',
            style: TextStyle(color: Theme.of(context).colorScheme.onSurface, fontSize: 16)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _dialogField('Monto diario máximo (COP)', diarCtrl, Icons.today_rounded, context),
            const SizedBox(height: 14),
            _dialogField('Monto por transacción máximo', txCtrl, Icons.receipt_long_rounded, context),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: Text('Cancelar', style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6))),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: primaryColor,
              foregroundColor: Theme.of(context).colorScheme.onSurface,
            ),
            onPressed: () async {
              Navigator.of(ctx).pop();
              try {
                await ref.read(operationLimitsProvider.notifier).update(
                      limit.tipoOperacion,
                      int.parse(diarCtrl.text),
                      int.parse(txCtrl.text),
                    );
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                    content: const Text('✓ Límites actualizados'),
                    backgroundColor: primaryColor,
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
            child: const Text('Guardar'),
          ),
        ],
      ),
    );
  }

  Widget _dialogField(String label, TextEditingController ctrl, IconData icon, BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    
    return TextField(
      controller: ctrl,
      keyboardType: TextInputType.number,
      style: TextStyle(color: Theme.of(context).colorScheme.onSurface),
      decoration: InputDecoration(
        labelText: label,
        labelStyle: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6), fontSize: 13),
        prefixIcon: Icon(icon, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4), size: 20),
        filled: true,
        fillColor: Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.06 : 0.03),
        border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
        enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide(color: Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.12 : 0.05))),
        focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide(color: Theme.of(context).colorScheme.primary, width: 1.5)),
      ),
    );
  }
}

class _LimitCard extends StatelessWidget {
  const _LimitCard({required this.limit, required this.onEdit});
  final OperationLimit limit;
  final VoidCallback onEdit;

  static final _typeColors = {
    'TRANSFER': const Color(0xFF0B5FFF),
    'WITHDRAWAL': const Color(0xFFFF6B00),
  };

  static final _typeIcons = {
    'TRANSFER': Icons.swap_horiz_rounded,
    'WITHDRAWAL': Icons.account_balance_wallet_rounded,
  };

  static final _typeNames = {
    'TRANSFER': 'Transferencia',
    'WITHDRAWAL': 'Retiro de Efectivo',
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
    final color = _typeColors[limit.tipoOperacion] ?? Theme.of(context).colorScheme.primary;
    final icon = _typeIcons[limit.tipoOperacion] ?? Icons.money_rounded;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.05 : 0.03),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: color.withOpacity(isDark ? 0.3 : 0.4)),
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
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _typeNames[limit.tipoOperacion] ?? limit.tipoOperacion,
                      style: TextStyle(
                          color: Theme.of(context).colorScheme.onSurface,
                          fontWeight: FontWeight.bold,
                          fontSize: 16),
                    ),
                    Text(
                      limit.tipoOperacion,
                      style: TextStyle(
                          color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4),
                          fontSize: 11,
                          letterSpacing: 0.5),
                    ),
                  ],
                ),
              ),
              IconButton(
                icon: Icon(Icons.edit_rounded, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5), size: 20),
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
        Icon(icon, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4), size: 16),
        const SizedBox(width: 8),
        Text(label, style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5), fontSize: 13)),
        const SizedBox(width: 8),
        Expanded(
          child: Text(value,
              textAlign: TextAlign.right,
              style: TextStyle(
                  color: Theme.of(context).colorScheme.onSurface, fontWeight: FontWeight.bold, fontSize: 14)),
        ),
      ],
    );
  }
}
