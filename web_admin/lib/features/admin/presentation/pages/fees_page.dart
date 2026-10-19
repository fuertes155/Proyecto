import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/ui/ui.dart';
import '../../data/models/admin_models.dart';
import '../providers/admin_provider.dart';
import '../shell/admin_nav.dart';

/// "TRANSFERENCIA_EXTERNA" → "Transferencia externa".
String _humanize(String code) {
  final words = code.toLowerCase().split('_').where((w) => w.isNotEmpty).toList();
  if (words.isEmpty) return code;
  final text = words.join(' ');
  return text[0].toUpperCase() + text.substring(1);
}

/// El backend guarda los porcentajes como fracción (0.02 = 2 %).
String _formatFee(FeeSchedule fee) {
  if (!fee.esPorcentaje) return formatCop(fee.valor);
  final pct = fee.valor * 100;
  final text = pct == pct.roundToDouble() ? pct.toStringAsFixed(0) : pct.toStringAsFixed(2);
  return '$text %';
}

class FeesPage extends ConsumerWidget {
  const FeesPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final feesState = ref.watch(feeScheduleProvider);

    return AdminPage(
      section: sectionTitleFor(feesItem.route),
      title: 'Tarifas y comisiones',
      subtitle: 'Cobros que la plataforma aplica a cada tipo de operación.',
      actions: [
        OutlinedButton.icon(
          onPressed: () => ref.read(feeScheduleProvider.notifier).load(),
          icon: const Icon(Icons.refresh_rounded, size: 18),
          label: const Text('Actualizar'),
        ),
        FilledButton.icon(
          onPressed: () => _openForm(context, ref),
          icon: const Icon(Icons.add_rounded, size: 20),
          label: const Text('Nueva tarifa'),
        ),
      ],
      children: [
        feesState.when(
          loading: () => const LoadingState(label: 'Cargando tarifas…'),
          error: (e, _) => ErrorState(error: e, onRetry: () => ref.read(feeScheduleProvider.notifier).load()),
          data: (fees) {
            if (fees.isEmpty) {
              return Card(
                child: EmptyState(
                  icon: Icons.percent_rounded,
                  title: 'No hay tarifas configuradas',
                  message: 'Crea la primera tarifa para empezar a cobrar comisiones.',
                  action: FilledButton.icon(
                    onPressed: () => _openForm(context, ref),
                    icon: const Icon(Icons.add_rounded, size: 20),
                    label: const Text('Nueva tarifa'),
                  ),
                ),
              );
            }
            return AdminTable<FeeSchedule>(
              rows: fees,
              columns: [
                AdminColumn(
                  label: 'Tarifa',
                  flex: 4,
                  cell: (f) => TwoLineCell(_humanize(f.tipoTarifa), f.descripcion ?? f.tipoTarifa,
                      leading: const IconBadge(Icons.percent_rounded, size: 36)),
                ),
                AdminColumn(label: 'Valor', flex: 2, cell: (f) => CellText(_formatFee(f), strong: true)),
                AdminColumn(
                  label: 'Tipo',
                  flex: 2,
                  cell: (f) => StatusBadge(
                    f.esPorcentaje ? const StatusInfo('Porcentaje', Tone.info) : const StatusInfo('Valor fijo', Tone.neutral),
                    showDot: false,
                  ),
                ),
                AdminColumn(
                  label: 'Vigencia',
                  flex: 3,
                  cell: (f) => CellText(
                    f.vigentaHasta == null
                        ? 'Desde ${formatDateTime(f.vigentDesde, withTime: false)}'
                        : '${formatDateTime(f.vigentDesde, withTime: false)} – ${formatDateTime(f.vigentaHasta, withTime: false)}',
                    muted: true,
                  ),
                ),
                AdminColumn(
                  label: 'Acciones',
                  flex: 2,
                  alignEnd: true,
                  cell: (f) => Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      IconButton(
                        tooltip: 'Editar tarifa',
                        onPressed: () => _openForm(context, ref, fee: f),
                        icon: const Icon(Icons.edit_outlined, size: 20),
                      ),
                      IconButton(
                        tooltip: 'Eliminar tarifa',
                        onPressed: () => _confirmDelete(context, ref, f),
                        icon: const Icon(Icons.delete_outline_rounded, size: 20, color: MetColors.danger),
                      ),
                    ],
                  ),
                ),
              ],
            );
          },
        ),
      ],
    );
  }

  Future<void> _openForm(BuildContext context, WidgetRef ref, {FeeSchedule? fee}) async {
    final data = await showDialog<Map<String, dynamic>>(
      context: context,
      builder: (_) => _FeeFormDialog(fee: fee),
    );
    if (data == null || !context.mounted) return;
    try {
      final notifier = ref.read(feeScheduleProvider.notifier);
      if (fee == null) {
        await notifier.createFee(data);
      } else {
        await notifier.updateFee(fee.id, data);
      }
      if (context.mounted) showMetSnack(context, fee == null ? 'Tarifa creada' : 'Tarifa actualizada');
    } catch (e) {
      if (context.mounted) showErrorSnack(context, e);
    }
  }

  Future<void> _confirmDelete(BuildContext context, WidgetRef ref, FeeSchedule fee) async {
    final confirmed = await confirmAction(
      context,
      title: 'Eliminar tarifa',
      message: 'La tarifa dejará de cobrarse en las operaciones nuevas. Esta acción queda registrada en la auditoría.',
      confirmLabel: 'Eliminar',
      destructive: true,
      icon: Icons.delete_outline_rounded,
      details: [('Tarifa', _humanize(fee.tipoTarifa)), ('Valor', _formatFee(fee))],
    );
    if (!confirmed || !context.mounted) return;
    try {
      await ref.read(feeScheduleProvider.notifier).deleteFee(fee.id);
      if (context.mounted) showMetSnack(context, 'Tarifa eliminada', tone: Tone.neutral);
    } catch (e) {
      if (context.mounted) showErrorSnack(context, e);
    }
  }
}

class _FeeFormDialog extends StatefulWidget {
  const _FeeFormDialog({this.fee});

  final FeeSchedule? fee;

  @override
  State<_FeeFormDialog> createState() => _FeeFormDialogState();
}

class _FeeFormDialogState extends State<_FeeFormDialog> {
  final _formKey = GlobalKey<FormState>();
  late final _code = TextEditingController(text: widget.fee?.tipoTarifa ?? '');
  late final _description = TextEditingController(text: widget.fee?.descripcion ?? '');
  late bool _isPercent = widget.fee?.esPorcentaje ?? true;
  late final _value = TextEditingController(text: _initialValue());

  String _initialValue() {
    final fee = widget.fee;
    if (fee == null) return '';
    final v = fee.esPorcentaje ? fee.valor * 100 : fee.valor;
    return v == v.roundToDouble() ? v.toStringAsFixed(0) : v.toString();
  }

  @override
  void dispose() {
    _code.dispose();
    _description.dispose();
    _value.dispose();
    super.dispose();
  }

  double? get _parsedValue => double.tryParse(_value.text.replaceAll(',', '.'));

  void _submit() {
    if (!_formKey.currentState!.validate()) return;
    final value = _parsedValue!;
    Navigator.pop(context, {
      'tipoTarifa': widget.fee?.tipoTarifa ?? _code.text.trim().toUpperCase().replaceAll(RegExp(r'\s+'), '_'),
      'descripcion': _description.text.trim(),
      'valor': _isPercent ? value / 100 : value,
      'esPorcentaje': _isPercent,
      'vigentDesde': DateTime.now().toUtc().toIso8601String(),
    });
  }

  @override
  Widget build(BuildContext context) {
    final editing = widget.fee != null;
    return AlertDialog(
      title: Text(editing ? 'Editar tarifa' : 'Nueva tarifa'),
      content: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 460, minWidth: 380),
        child: Form(
          key: _formKey,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const FieldLabel('Código', required: true, help: 'Identifica la operación. Ej.: TRANSFERENCIA_EXTERNA'),
                TextFormField(
                  controller: _code,
                  enabled: !editing,
                  textCapitalization: TextCapitalization.characters,
                  decoration: const InputDecoration(hintText: 'TRANSFERENCIA_EXTERNA'),
                  validator: (v) => (v == null || v.trim().isEmpty) ? 'Escribe el código de la tarifa' : null,
                ),
                const SizedBox(height: 16),
                const FieldLabel('Descripción', help: 'Opcional. Cómo la verán los administradores.'),
                TextFormField(
                  controller: _description,
                  decoration: const InputDecoration(hintText: 'Transferencia a otros bancos'),
                ),
                const SizedBox(height: 16),
                const FieldLabel('Tipo de cobro'),
                SegmentedButton<bool>(
                  showSelectedIcon: false,
                  segments: const [
                    ButtonSegment(value: true, icon: Icon(Icons.percent_rounded, size: 18), label: Text('Porcentaje')),
                    ButtonSegment(value: false, icon: Icon(Icons.attach_money_rounded, size: 18), label: Text('Valor fijo')),
                  ],
                  selected: {_isPercent},
                  onSelectionChanged: (s) => setState(() => _isPercent = s.first),
                  style: SegmentedButton.styleFrom(
                    selectedBackgroundColor: MetColors.successBg,
                    selectedForegroundColor: MetColors.primary,
                    side: const BorderSide(color: MetColors.borderStrong),
                  ),
                ),
                const SizedBox(height: 16),
                FieldLabel(_isPercent ? 'Porcentaje' : 'Valor en pesos', required: true,
                    help: _isPercent ? 'Escribe 2 para cobrar el 2 %.' : 'Monto fijo por operación.'),
                TextFormField(
                  controller: _value,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]'))],
                  decoration: InputDecoration(
                    prefixIcon: Icon(_isPercent ? Icons.percent_rounded : Icons.attach_money_rounded),
                    suffixText: _isPercent ? '%' : 'COP',
                  ),
                  validator: (_) {
                    final v = _parsedValue;
                    if (v == null) return 'Escribe un número válido';
                    if (v < 0) return 'No puede ser negativo';
                    if (_isPercent && v > 100) return 'Un porcentaje no puede superar 100';
                    return null;
                  },
                  onFieldSubmitted: (_) => _submit(),
                ),
              ],
            ),
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          style: TextButton.styleFrom(foregroundColor: MetColors.textMuted),
          child: const Text('Cancelar'),
        ),
        FilledButton(onPressed: _submit, child: Text(editing ? 'Guardar cambios' : 'Crear tarifa')),
      ],
    );
  }
}
