import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/widgets/accessible_button.dart';
import '../../data/models/scheduled_savings_models.dart';
import '../providers/scheduled_savings_provider.dart';

class CreateScheduledSavingsPage extends ConsumerStatefulWidget {
  const CreateScheduledSavingsPage({super.key});

  @override
  ConsumerState<CreateScheduledSavingsPage> createState() => _CreateScheduledSavingsPageState();
}

class _CreateScheduledSavingsPageState extends ConsumerState<CreateScheduledSavingsPage> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _contributionController = TextEditingController();
  final _targetController = TextEditingController();
  String _frequency = 'MONTHLY';
  int _debitDayOfMonth = 1;
  int _debitDayOfWeek = 1;
  bool _isLoading = false;

  @override
  void dispose() {
    _nameController.dispose();
    _contributionController.dispose();
    _targetController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _isLoading = true);

    try {
      await ref.read(scheduledSavingsListProvider.notifier).create(
            CreateScheduledSavingsRequest(
              name: _nameController.text.trim(),
              contributionAmount: double.parse(_contributionController.text),
              targetAmount: _targetController.text.isEmpty
                  ? null
                  : double.parse(_targetController.text),
              frequency: _frequency,
              debitDayOfWeek: _frequency == 'MONTHLY' ? null : _debitDayOfWeek,
              debitDayOfMonth: _frequency == 'MONTHLY' ? _debitDayOfMonth : null,
            ),
          );
      if (!mounted) return;
      context.go('/savings/scheduled');
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.toString())));
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Nueva meta de ahorro')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextFormField(
                  controller: _nameController,
                  decoration: const InputDecoration(
                    labelText: 'Nombre de la meta',
                    hintText: 'Ej: Vacaciones 2026',
                  ),
                  validator: (v) => v == null || v.isEmpty ? 'Requerido' : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _contributionController,
                  decoration: const InputDecoration(
                    labelText: 'Monto del aporte automático',
                    prefixText: '\$ ',
                  ),
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  validator: (v) {
                    final value = double.tryParse(v ?? '');
                    if (value == null || value < 1000) return 'Mínimo \$1.000';
                    return null;
                  },
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _targetController,
                  decoration: const InputDecoration(
                    labelText: 'Meta total (opcional)',
                    prefixText: '\$ ',
                  ),
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                ),
                const SizedBox(height: 16),
                DropdownButtonFormField<String>(
                  value: _frequency,
                  decoration: const InputDecoration(labelText: 'Frecuencia del aporte'),
                  items: const [
                    DropdownMenuItem(value: 'WEEKLY', child: Text('Semanal')),
                    DropdownMenuItem(value: 'BIWEEKLY', child: Text('Quincenal')),
                    DropdownMenuItem(value: 'MONTHLY', child: Text('Mensual')),
                  ],
                  onChanged: (v) => setState(() => _frequency = v ?? 'MONTHLY'),
                ),
                const SizedBox(height: 16),
                if (_frequency == 'MONTHLY')
                  DropdownButtonFormField<int>(
                    value: _debitDayOfMonth,
                    decoration: const InputDecoration(labelText: 'Día del mes del débito'),
                    items: List.generate(
                      28,
                      (i) => DropdownMenuItem(value: i + 1, child: Text('Día ${i + 1}')),
                    ),
                    onChanged: (v) => setState(() => _debitDayOfMonth = v ?? 1),
                  )
                else
                  DropdownButtonFormField<int>(
                    value: _debitDayOfWeek,
                    decoration: const InputDecoration(labelText: 'Día de la semana del débito'),
                    items: const [
                      DropdownMenuItem(value: 1, child: Text('Lunes')),
                      DropdownMenuItem(value: 2, child: Text('Martes')),
                      DropdownMenuItem(value: 3, child: Text('Miércoles')),
                      DropdownMenuItem(value: 4, child: Text('Jueves')),
                      DropdownMenuItem(value: 5, child: Text('Viernes')),
                    ],
                    onChanged: (v) => setState(() => _debitDayOfWeek = v ?? 1),
                  ),
                const SizedBox(height: 32),
                AccessibleButton(
                  label: 'Crear meta de ahorro',
                  isLoading: _isLoading,
                  onPressed: _submit,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
