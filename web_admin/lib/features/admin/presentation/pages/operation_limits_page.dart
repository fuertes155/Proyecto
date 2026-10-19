import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/ui/ui.dart';
import '../../data/models/admin_models.dart';
import '../providers/admin_provider.dart';
import '../shell/admin_nav.dart';

const _typeNames = {
  'TRANSFER': 'Transferencias',
  'WITHDRAWAL': 'Retiros',
};
const _typeDescriptions = {
  'TRANSFER': 'Envíos entre socios y a otras entidades.',
  'WITHDRAWAL': 'Retiros de saldo hacia cuentas bancarias.',
};
const _typeIcons = {
  'TRANSFER': Icons.swap_horiz_rounded,
  'WITHDRAWAL': Icons.account_balance_rounded,
};

class OperationLimitsPage extends ConsumerWidget {
  const OperationLimitsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final limitsState = ref.watch(operationLimitsProvider);

    return AdminPage(
      section: sectionTitleFor(limitsItem.route),
      title: 'Límites de operación',
      subtitle: 'Topes máximos que un socio puede mover por día y en cada transacción.',
      actions: [
        OutlinedButton.icon(
          onPressed: () => ref.read(operationLimitsProvider.notifier).load(),
          icon: const Icon(Icons.refresh_rounded, size: 18),
          label: const Text('Actualizar'),
        ),
      ],
      children: [
        limitsState.when(
          loading: () => const LoadingState(label: 'Cargando límites…'),
          error: (e, _) => ErrorState(error: e, onRetry: () => ref.read(operationLimitsProvider.notifier).load()),
          data: (limits) {
            // Excluir PURCHASE y deduplicar por tipoOperacion
            final seen = <String>{};
            final filtered = limits
                .where((l) => l.tipoOperacion != 'PURCHASE')
                .where((l) => seen.add(l.tipoOperacion))
                .toList();
            if (filtered.isEmpty) {
              return const EmptyState(icon: Icons.tune_rounded, title: 'No hay límites configurados');
            }
            return Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const CalloutBanner(
                  message: 'Los cambios aplican de inmediato a todas las operaciones nuevas y quedan registrados en la auditoría.',
                ),
                const SizedBox(height: 20),
                ResponsiveGrid(
                  minItemWidth: 380,
                  maxColumns: 2,
                  children: [
                    for (final limit in filtered)
                      _LimitCard(limit: limit, onEdit: () => _showEditDialog(context, ref, limit)),
                  ],
                ),
              ],
            );
          },
        ),
      ],
    );
  }

  Future<void> _showEditDialog(BuildContext context, WidgetRef ref, OperationLimit limit) async {
    final result = await showDialog<(int, int)>(
      context: context,
      builder: (_) => _EditLimitDialog(limit: limit),
    );
    if (result == null || !context.mounted) return;
    try {
      await ref.read(operationLimitsProvider.notifier).update(limit.tipoOperacion, result.$1, result.$2);
      if (context.mounted) showMetSnack(context, 'Límites de ${(_typeNames[limit.tipoOperacion] ?? limit.tipoOperacion).toLowerCase()} actualizados');
    } catch (e) {
      if (context.mounted) showErrorSnack(context, e);
    }
  }
}

class _LimitCard extends StatelessWidget {
  const _LimitCard({required this.limit, required this.onEdit});

  final OperationLimit limit;
  final VoidCallback onEdit;

  @override
  Widget build(BuildContext context) {
    final tone = limit.tipoOperacion == 'WITHDRAWAL' ? Tone.warning : Tone.info;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(22),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                IconBadge(_typeIcons[limit.tipoOperacion] ?? Icons.payments_rounded, tone: tone),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(_typeNames[limit.tipoOperacion] ?? limit.tipoOperacion,
                          style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700)),
                      Text(_typeDescriptions[limit.tipoOperacion] ?? limit.tipoOperacion,
                          style: const TextStyle(fontSize: 13.5, color: MetColors.textMuted)),
                    ],
                  ),
                ),
                StatusBadge(limit.activo ? const StatusInfo('Vigente', Tone.success) : const StatusInfo('Inactivo', Tone.neutral)),
              ],
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(child: _Amount(label: 'Máximo diario', value: limit.montoDiarioMax)),
                Container(width: 1, height: 48, color: MetColors.border),
                const SizedBox(width: 20),
                Expanded(child: _Amount(label: 'Máximo por transacción', value: limit.montoPorTransaccionMax)),
              ],
            ),
            const SizedBox(height: 18),
            Align(
              alignment: Alignment.centerRight,
              child: OutlinedButton.icon(
                onPressed: onEdit,
                icon: const Icon(Icons.edit_rounded, size: 18),
                label: const Text('Editar límites'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Amount extends StatelessWidget {
  const _Amount({required this.label, required this.value});

  final String label;
  final int value;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 13, color: MetColors.textMuted)),
        const SizedBox(height: 4),
        FittedBox(
          fit: BoxFit.scaleDown,
          alignment: Alignment.centerLeft,
          child: Text(formatCop(value.toDouble()),
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800, letterSpacing: -0.3)),
        ),
      ],
    );
  }
}

class _EditLimitDialog extends StatefulWidget {
  const _EditLimitDialog({required this.limit});

  final OperationLimit limit;

  @override
  State<_EditLimitDialog> createState() => _EditLimitDialogState();
}

class _EditLimitDialogState extends State<_EditLimitDialog> {
  final _formKey = GlobalKey<FormState>();
  late final _daily = TextEditingController(text: widget.limit.montoDiarioMax.toString());
  late final _perTx = TextEditingController(text: widget.limit.montoPorTransaccionMax.toString());

  @override
  void dispose() {
    _daily.dispose();
    _perTx.dispose();
    super.dispose();
  }

  String? _validateAmount(String? v) {
    final n = int.tryParse(v ?? '');
    if (n == null) return 'Escribe un monto en pesos, sin puntos';
    if (n <= 0) return 'Debe ser mayor a cero';
    return null;
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text('Editar límites de ${(_typeNames[widget.limit.tipoOperacion] ?? widget.limit.tipoOperacion).toLowerCase()}'),
      content: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 420, minWidth: 360),
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const FieldLabel('Máximo diario (COP)', required: true),
              _AmountField(controller: _daily, validator: _validateAmount),
              const SizedBox(height: 16),
              const FieldLabel('Máximo por transacción (COP)', required: true),
              _AmountField(
                controller: _perTx,
                validator: (v) {
                  final base = _validateAmount(v);
                  if (base != null) return base;
                  final daily = int.tryParse(_daily.text);
                  if (daily != null && int.parse(v!) > daily) return 'No puede superar el máximo diario';
                  return null;
                },
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          style: TextButton.styleFrom(foregroundColor: MetColors.textMuted),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          onPressed: () {
            if (!_formKey.currentState!.validate()) return;
            Navigator.pop(context, (int.parse(_daily.text), int.parse(_perTx.text)));
          },
          child: const Text('Guardar cambios'),
        ),
      ],
    );
  }
}

class _AmountField extends StatelessWidget {
  const _AmountField({required this.controller, required this.validator});

  final TextEditingController controller;
  final FormFieldValidator<String> validator;

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<TextEditingValue>(
      valueListenable: controller,
      builder: (context, value, _) {
        final n = int.tryParse(value.text);
        return TextFormField(
          controller: controller,
          keyboardType: TextInputType.number,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          validator: validator,
          decoration: InputDecoration(
            prefixIcon: const Icon(Icons.attach_money_rounded),
            helperText: n == null ? null : formatCop(n.toDouble()),
          ),
        );
      },
    );
  }
}
