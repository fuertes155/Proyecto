import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../providers/investment_providers.dart';

/// Muestra un resumen agregado de en qué está trabajando el capital del
/// usuario dentro del motor de distribución P2P. A propósito NO muestra a
/// qué socios concretos quedó emparejado cada fracción: la identidad de otro
/// socio es información sensible y solo la ve un administrador.
class InvestmentBreakdownPage extends ConsumerWidget {
  const InvestmentBreakdownPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final summaryAsync = ref.watch(investmentSummaryProvider);
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
        onRefresh: () => ref.refresh(investmentSummaryProvider.future),
        child: summaryAsync.when(
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
                  onPressed: () => ref.invalidate(investmentSummaryProvider),
                  child: const Text('Reintentar'),
                ),
              ),
            ],
          ),
          data: (summary) {
            if (summary.totalInvested <= 0) {
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
                        'Cuando hagas un depósito, verás aquí un resumen de en qué créditos de la cooperativa está trabajando tu capital.',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.black38),
                      ),
                    ),
                  ),
                ],
              );
            }

            final cards = <_SummaryCardData>[
              if (summary.activeAmount > 0)
                _SummaryCardData(
                  icon: Icons.trending_up,
                  color: const Color(0xFF2E7D32),
                  title: 'Generando rendimiento',
                  subtitle: summary.loansFundedCount > 0
                      ? 'Financiando a ${summary.loansFundedCount} socio(s) de la cooperativa'
                      : 'Activo en la cooperativa',
                  amount: summary.activeAmount,
                ),
              if (summary.availableAmount > 0)
                _SummaryCardData(
                  icon: Icons.pool,
                  color: const Color(0xFF1565C0),
                  title: 'Disponible por asignar',
                  subtitle: 'Esperando emparejarse con un nuevo crédito',
                  amount: summary.availableAmount,
                ),
              if (summary.paidOffAmount > 0)
                _SummaryCardData(
                  icon: Icons.check_circle_outline,
                  color: Colors.grey.shade700,
                  title: 'Recuperado',
                  subtitle: 'Créditos que ya devolvieron el capital',
                  amount: summary.paidOffAmount,
                ),
              if (summary.returnedAmount > 0)
                _SummaryCardData(
                  icon: Icons.shield_outlined,
                  color: const Color(0xFF6A1B9A),
                  title: 'Cubierto por el fondo de garantías',
                  subtitle: 'Protegido ante mora prolongada',
                  amount: summary.returnedAmount,
                ),
            ];

            return ListView(
              padding: const EdgeInsets.all(16),
              children: [
                const Padding(
                  padding: EdgeInsets.only(bottom: 16),
                  child: Text(
                    'Tu dinero se ha fraccionado dinámicamente entre distintos créditos de la cooperativa. Por seguridad, no mostramos la identidad de los socios que reciben el financiamiento.',
                    style: TextStyle(fontSize: 15, color: Colors.black54),
                  ),
                ),
                Container(
                  margin: const EdgeInsets.only(bottom: 16),
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: const Color(0xFF2C3545),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Total invertido', style: TextStyle(color: Colors.white70)),
                      Text(currency.format(summary.totalInvested),
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 18)),
                    ],
                  ),
                ),
                for (int i = 0; i < cards.length; i++)
                  _SummaryCard(data: cards[i], currency: currency, index: i),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _SummaryCardData {
  const _SummaryCardData({
    required this.icon,
    required this.color,
    required this.title,
    required this.subtitle,
    required this.amount,
  });

  final IconData icon;
  final Color color;
  final String title;
  final String subtitle;
  final double amount;
}

class _SummaryCard extends StatelessWidget {
  const _SummaryCard({required this.data, required this.currency, required this.index});

  final _SummaryCardData data;
  final NumberFormat currency;
  final int index;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 5, offset: const Offset(0, 2))],
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(color: data.color.withOpacity(0.1), shape: BoxShape.circle),
            child: Icon(data.icon, color: data.color),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(data.title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                const SizedBox(height: 4),
                Text(data.subtitle,
                    style: TextStyle(color: Colors.black54, fontSize: 13)),
              ],
            ),
          ),
          Text(currency.format(data.amount),
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Color(0xFF2C3545))),
        ],
      ),
    ).animate().fadeIn(delay: Duration(milliseconds: 80 * index)).slideX();
  }
}
