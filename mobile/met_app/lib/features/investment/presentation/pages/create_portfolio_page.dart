import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../core/theme/app_theme.dart';
import '../../data/models/investment_models.dart';
import '../providers/investment_providers.dart';

/// Pantalla para crear un nuevo portfolio de micro-inversiones.
/// Permite elegir monto y estrategia, y muestra una vista previa de la distribución.
class CreatePortfolioPage extends ConsumerStatefulWidget {
  const CreatePortfolioPage({super.key});

  @override
  ConsumerState<CreatePortfolioPage> createState() =>
      _CreatePortfolioPageState();
}

class _CreatePortfolioPageState extends ConsumerState<CreatePortfolioPage> {
  final _formKey = GlobalKey<FormState>();
  final _amountController = TextEditingController(text: '100000');
  bool _isLoading = false;

  final _currencyFormat =
      NumberFormat.currency(locale: 'es_CO', symbol: '\$', decimalDigits: 0);

  static final _strategies = [
    (value: 'EQUAL', label: 'Igualitaria', desc: 'Divide el monto en partes iguales', icon: Icons.balance),
    (value: 'WEIGHTED', label: 'Ponderada', desc: 'Mayor dinero a los mejores rendimientos', icon: Icons.trending_up),
    (value: 'RISK_BASED', label: 'Por Riesgo', desc: 'Más dinero en instrumentos conservadores', icon: Icons.shield_outlined),
  ];

  @override
  void dispose() {
    _amountController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    final amount = double.tryParse(
            _amountController.text.replaceAll('.', '').replaceAll(',', '')) ??
        0;
    if (amount < 5000) {
      _showError('El monto mínimo es \$5.000');
      return;
    }

    final strategy = ref.read(selectedStrategyProvider);

    setState(() => _isLoading = true);
    try {
      final portfolio = await ref
          .read(investmentPortfoliosProvider.notifier)
          .createPortfolio(
              CreatePortfolioRequest(montoTotal: amount, estrategia: strategy));

      if (mounted) {
        context.pushReplacement('/investments/portfolio/${portfolio.id}');
      }
    } catch (e) {
      if (mounted) _showError(e.toString());
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showError(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
          content: Text(msg),
          backgroundColor: Theme.of(context).colorScheme.error),
    );
  }

  @override
  Widget build(BuildContext context) {
    final selectedStrategy = ref.watch(selectedStrategyProvider);
    final instrumentsAsync = ref.watch(investmentInstrumentsProvider);

    return Scaffold(
      
      appBar: AppBar(
        
        foregroundColor: Theme.of(context).colorScheme.onSurface,
        title: Text('Crear Portfolio',
            style: TextStyle(fontWeight: FontWeight.bold)),
        elevation: 0,
      ),
      body: Form(
        key: _formKey,
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // ── Instrucciones ─────────────────────────────────────────
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppTheme.primaryColor.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                      color: AppTheme.primaryColor.withOpacity(0.3)),
                ),
                child: Row(
                  children: [
                    Icon(Icons.info_outline,
                        color: AppTheme.primaryColor, size: 20),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        'El sistema distribuirá tu dinero entre los instrumentos disponibles según la estrategia que elijas.',
                        style: TextStyle(
                            color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7), fontSize: 13, height: 1.4),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 28),

              // ── Monto ─────────────────────────────────────────────────
              Text('¿Cuánto quieres invertir?',
                  style: TextStyle(
                      color: Theme.of(context).colorScheme.onSurface,
                      fontSize: 18,
                      fontWeight: FontWeight.bold)),
              const SizedBox(height: 12),
              TextFormField(
                controller: _amountController,
                keyboardType: TextInputType.number,
                style: TextStyle(
                    color: Theme.of(context).colorScheme.onSurface, fontSize: 24, fontWeight: FontWeight.bold),
                decoration: InputDecoration(
                  prefixText: '\$ ',
                  prefixStyle: TextStyle(
                      color: AppTheme.primaryColor,
                      fontSize: 24,
                      fontWeight: FontWeight.bold),
                  hintText: '100,000',
                  hintStyle: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.24)),
                  filled: true,
                  fillColor: Theme.of(context).colorScheme.surfaceContainer,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(14),
                    borderSide: BorderSide.none,
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(14),
                    borderSide: const BorderSide(
                        color: AppTheme.primaryColor, width: 2),
                  ),
                ),
                validator: (v) {
                  if (v == null || v.isEmpty) return 'Ingresa un monto';
                  final n = double.tryParse(
                      v.replaceAll('.', '').replaceAll(',', ''));
                  if (n == null || n < 5000) return 'Mínimo \$5.000';
                  return null;
                },
              ),

              const SizedBox(height: 32),

              // ── Estrategia ────────────────────────────────────────────
              Text('Estrategia de distribución',
                  style: TextStyle(
                      color: Theme.of(context).colorScheme.onSurface,
                      fontSize: 18,
                      fontWeight: FontWeight.bold)),
              const SizedBox(height: 12),
              ..._strategies.map((s) => _StrategyTile(
                    value: s.value,
                    label: s.label,
                    description: s.desc,
                    icon: s.icon,
                    isSelected: selectedStrategy == s.value,
                    onTap: () => ref
                        .read(selectedStrategyProvider.notifier)
                        .state = s.value,
                  )),

              const SizedBox(height: 28),

              // ── Instrumentos disponibles preview ──────────────────────
              instrumentsAsync.when(
                loading: () => const SizedBox.shrink(),
                error: (_, __) => const SizedBox.shrink(),
                data: (instruments) {
                  if (instruments.isEmpty) return const SizedBox.shrink();
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Se distribuirá entre ${instruments.length} instrumento(s)',
                        style: TextStyle(
                            color: Theme.of(context).colorScheme.onSurface.withOpacity(0.54), fontSize: 13),
                      ),
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: instruments
                            .map((i) => Chip(
                                  label: Text(
                                    '${i.nombre} · ${i.tasaLabel}',
                                    style: TextStyle(
                                        fontSize: 11, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)),
                                  ),
                                  backgroundColor:
                                      Theme.of(context).colorScheme.surfaceContainer,
                                  side: BorderSide.none,
                                ))
                            .toList(),
                      ),
                      const SizedBox(height: 20),
                    ],
                  );
                },
              ),

              // ── Botón Invertir ────────────────────────────────────────
              SizedBox(
                width: double.infinity,
                height: 56,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _submit,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primaryColor,
                    foregroundColor: Theme.of(context).colorScheme.onSurface,
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14)),
                    elevation: 4,
                  ),
                  child: _isLoading
                      ? SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(
                              color: Theme.of(context).colorScheme.onSurface, strokeWidth: 2.5))
                      : const Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.rocket_launch_outlined, size: 20),
                            SizedBox(width: 8),
                            Text('Invertir Ahora',
                                style: TextStyle(
                                    fontSize: 18,
                                    fontWeight: FontWeight.bold)),
                          ],
                        ),
                ),
              ),
              const SizedBox(height: 32),
            ],
          ),
        ),
      ),
    );
  }
}

class _StrategyTile extends StatelessWidget {
  const _StrategyTile({
    required this.value,
    required this.label,
    required this.description,
    required this.icon,
    required this.isSelected,
    required this.onTap,
  });

  final String value;
  final String label;
  final String description;
  final IconData icon;
  final bool isSelected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        margin: const EdgeInsets.only(bottom: 10),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          color: isSelected
              ? AppTheme.primaryColor.withOpacity(0.15)
              : Theme.of(context).colorScheme.surfaceContainer,
          border: Border.all(
            color: isSelected
                ? AppTheme.primaryColor
                : Theme.of(context).colorScheme.onSurface.withOpacity(0.08),
            width: isSelected ? 2 : 1,
          ),
        ),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: (isSelected
                        ? AppTheme.primaryColor
                        : Theme.of(context).colorScheme.onSurface.withOpacity(0.24))
                    .withOpacity(0.2),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(icon,
                  color: isSelected
                      ? AppTheme.primaryColor
                      : Theme.of(context).colorScheme.onSurface.withOpacity(0.38),
                  size: 22),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(label,
                      style: TextStyle(
                          color: isSelected
                              ? AppTheme.primaryColor
                              : Theme.of(context).colorScheme.onSurface,
                          fontWeight: FontWeight.bold,
                          fontSize: 15)),
                  Text(description,
                      style: TextStyle(
                          color: Theme.of(context).colorScheme.onSurface.withOpacity(0.54), fontSize: 12)),
                ],
              ),
            ),
            if (isSelected)
              const Icon(Icons.check_circle,
                  color: AppTheme.primaryColor, size: 22),
          ],
        ),
      ),
    );
  }
}
