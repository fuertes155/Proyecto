import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/utils/currency_formatter.dart';
import '../../../../core/widgets/accessible_button.dart';
import '../../data/datasources/loan_remote_datasource.dart';
import '../../data/models/loan_models.dart';
import '../providers/loan_provider.dart';

class LoanSimulationPage extends ConsumerStatefulWidget {
  const LoanSimulationPage({super.key});

  @override
  ConsumerState<LoanSimulationPage> createState() => _LoanSimulationPageState();
}

class _LoanSimulationPageState extends ConsumerState<LoanSimulationPage> {
  final _formKey = GlobalKey<FormState>();
  final _amountController = TextEditingController(text: '5000000');
  final _purposeController = TextEditingController();
  int _termMonths = 24;
  double _annualRate = 0.24;
  bool _isSubmitting = false;

  @override
  void dispose() {
    _amountController.dispose();
    _purposeController.dispose();
    super.dispose();
  }

  Future<void> _simulate() async {
    if (!_formKey.currentState!.validate()) return;
    await ref.read(loanSimulationProvider.notifier).simulate(
          SimulateLoanRequest(
            amount: double.parse(_amountController.text),
            termMonths: _termMonths,
            annualInterestRate: _annualRate,
          ),
        );
  }

  Future<void> _submitApplication() async {
    if (_purposeController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Indica el propósito del préstamo')),
      );
      return;
    }
    setState(() => _isSubmitting = true);
    try {
      final app = await ref.read(loanRemoteDataSourceProvider).submitApplication(
            SubmitLoanApplicationRequest(
              amount: double.parse(_amountController.text),
              termMonths: _termMonths,
              annualInterestRate: _annualRate,
              purpose: _purposeController.text.trim(),
            ),
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Solicitud enviada correctamente')),
      );
      context.push('/loans/applications/${app.id}');
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final simulation = ref.watch(loanSimulationProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Simular préstamo'),
        actions: [
          IconButton(
            icon: const Icon(Icons.list_alt),
            tooltip: 'Mis solicitudes',
            onPressed: () => context.push('/loans/applications'),
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextFormField(
                  controller: _amountController,
                  decoration: const InputDecoration(
                    labelText: 'Monto a solicitar',
                    prefixText: '\$ ',
                  ),
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  validator: (v) {
                    final val = double.tryParse(v ?? '');
                    if (val == null || val < 500000) return 'Mínimo \$500.000';
                    if (val > 50000000) return 'Máximo \$50.000.000';
                    return null;
                  },
                ),
                const SizedBox(height: 16),
                DropdownButtonFormField<int>(
                  value: _termMonths,
                  decoration: const InputDecoration(labelText: 'Plazo (meses)'),
                  items: const [6, 12, 18, 24, 36, 48, 60]
                      .map((m) => DropdownMenuItem(value: m, child: Text('$m meses')))
                      .toList(),
                  onChanged: (v) => setState(() => _termMonths = v ?? 24),
                ),
                const SizedBox(height: 16),
                Text('Tasa EA: ${(_annualRate * 100).toStringAsFixed(1)}%'),
                Slider(
                  value: _annualRate,
                  min: 0.12,
                  max: 0.36,
                  divisions: 12,
                  label: '${(_annualRate * 100).toStringAsFixed(1)}%',
                  onChanged: (v) => setState(() => _annualRate = v),
                ),
                const SizedBox(height: 16),
                AccessibleButton(label: 'Simular', onPressed: _simulate),
                const SizedBox(height: 24),
                simulation.when(
                  loading: () => const Center(child: CircularProgressIndicator()),
                  error: (e, _) => Text('Error: $e'),
                  data: (result) {
                    if (result == null) return const SizedBox.shrink();
                    return _SimulationResult(
                      result: result,
                      purposeController: _purposeController,
                      isSubmitting: _isSubmitting,
                      onSubmit: _submitApplication,
                    );
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _SimulationResult extends StatelessWidget {
  const _SimulationResult({
    required this.result,
    required this.purposeController,
    required this.isSubmitting,
    required this.onSubmit,
  });

  final LoanSimulationResult result;
  final TextEditingController purposeController;
  final bool isSubmitting;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text('Resultado de la simulación',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600)),
            const SizedBox(height: 16),
            _SummaryRow('Cuota mensual', formatCop(result.monthlyPayment)),
            _SummaryRow('Total intereses', formatCop(result.totalInterest)),
            _SummaryRow('Total a pagar', formatCop(result.totalPayment)),
            const SizedBox(height: 16),
            const Text('Tabla de amortización (sistema francés)',
                style: TextStyle(fontWeight: FontWeight.w600)),
            const SizedBox(height: 8),
            ...result.schedule.take(3).map(
                  (i) => ListTile(
                    dense: true,
                    title: Text('Cuota ${i.installmentNumber}: ${formatCop(i.paymentAmount)}'),
                    subtitle: Text(
                      'Capital: ${formatCop(i.principalAmount)} · Interés: ${formatCop(i.interestAmount)}',
                    ),
                  ),
                ),
            if (result.schedule.length > 3)
              Text('... y ${result.schedule.length - 3} cuotas más',
                  style: TextStyle(color: Colors.grey.shade700)),
            const SizedBox(height: 16),
            TextField(
              controller: purposeController,
              decoration: const InputDecoration(
                labelText: 'Propósito del préstamo',
                hintText: 'Ej: Mejoras en vivienda',
              ),
            ),
            const SizedBox(height: 16),
            AccessibleButton(
              label: 'Solicitar préstamo',
              isLoading: isSubmitting,
              onPressed: onSubmit,
            ),
          ],
        ),
      ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow(this.label, this.value);
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(fontSize: 16)),
          Text(value, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}
