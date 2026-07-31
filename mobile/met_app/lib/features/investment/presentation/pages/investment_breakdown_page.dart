import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../data/models/investment_models.dart';
import '../providers/investment_providers.dart';

/// Muestra, con datos reales del motor de distribución P2P, en qué préstamos
/// concretos (o en el fondo de liquidez, mientras espera emparejamiento) está
/// trabajando el capital que el usuario ha depositado.
class InvestmentBreakdownPage extends ConsumerWidget {
  const InvestmentBreakdownPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final breakdownAsync = ref.watch(investmentBreakdownProvider);
    final currency =
        NumberFormat.currency(locale: 'es_CO', symbol: '\$', decimalDigits: 0);

    return Scaffold(
      backgroundColor: const Color(0xFFF0F4F8),
      appBar: AppBar(
        title: const Text('Mis Inversiones', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: const Color(0xFFF0F4F8),
        elevation: 0,
        foregroundColor: Colors.black87,
      ),
      body: RefreshIndicator(
        onRefresh: () => ref.refresh(investmentBreakdownProvider.future),
        child: breakdownAsync.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (e, _) => ListView(
            children: [
              const SizedBox(height: 120),
              const Icon(Icons.error_outline, size: 48, color: Colors.black38),
              const SizedBox(height: 12),
              const Center(child: Text('No se pudo cargar tu inversión.', style: TextStyle(color: Colors.black54))),
              const SizedBox(height: 12),
              Center(
                child: TextButton(
                  onPressed: () => ref.invalidate(investmentBreakdownProvider),
                  child: const Text('Reintentar'),
                ),
              ),
            ],
          ),
          data: (items) {
            if (items.isEmpty) {
              return ListView(
                children: [
                  const SizedBox(height: 120),
                  const Icon(Icons.savings_outlined, size: 64, color: Colors.black26),
                  const SizedBox(height: 16),
                  const Center(
                    child: Text('Aún no tienes dinero trabajando', style: TextStyle(fontSize: 18, color: Colors.black54)),
                  ),
                  const SizedBox(height: 8),
                  const Center(
                    child: Padding(
                      padding: EdgeInsets.symmetric(horizontal: 32),
                      child: Text(
                        'Cuando hagas un depósito, verás aquí en qué créditos de la cooperativa está trabajando tu capital.',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.black38),
                      ),
                    ),
                  ),
                ],
              );
            }

            final total = items.fold<double>(0, (sum, i) => sum + i.amount);

            return ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: items.length + 2,
              itemBuilder: (context, index) {
                if (index == 0) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 16),
                    child: Text(
                      'Tu dinero se ha fraccionado dinámicamente y se encuentra fondeando los siguientes créditos en la cooperativa:',
                      style: TextStyle(fontSize: 16, color: Colors.black54),
                    ),
                  );
                }
                if (index == 1) {
                  return Container(
                    margin: const EdgeInsets.only(bottom: 16),
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: const Color(0xFF2C3545),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('Total distribuido', style: TextStyle(color: Colors.white70)),
                        Text(currency.format(total),
                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 18)),
                      ],
                    ),
                  );
                }

                final item = items[index - 2];
                return _BreakdownCard(item: item, currency: currency, index: index);
              },
            );
          },
        ),
      ),
    );
  }
}

class _BreakdownCard extends StatelessWidget {
  const _BreakdownCard({required this.item, required this.currency, required this.index});

  final InvestmentBreakdownItem item;
  final NumberFormat currency;
  final int index;

  @override
  Widget build(BuildContext context) {
    final isLate = item.status == 'DEVUELTO';
    final color = switch (item.status) {
      'DISPONIBLE' => Colors.blue,
      'PAGADO' => Colors.grey,
      'DEVUELTO' => Colors.red,
      _ => Colors.green,
    };

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: isLate ? Colors.red.withOpacity(0.3) : Colors.transparent),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 5, offset: const Offset(0, 2))],
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(color: color.withOpacity(0.1), shape: BoxShape.circle),
            child: Icon(item.isLiquidityFund ? Icons.pool : Icons.person, color: color),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.isLiquidityFund ? item.borrowerName : 'Fondeando a ${item.borrowerName}',
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                ),
                const SizedBox(height: 4),
                Text('Estado: ${item.statusLabel}',
                    style: TextStyle(color: color, fontWeight: FontWeight.w600, fontSize: 13)),
              ],
            ),
          ),
          Text(currency.format(item.amount),
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Color(0xFF2C3545))),
        ],
      ),
    ).animate().fadeIn(delay: Duration(milliseconds: 60 * index)).slideX();
  }
}
