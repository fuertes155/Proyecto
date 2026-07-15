import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../core/theme/app_theme.dart';
import '../../data/models/investment_models.dart';
import '../providers/investment_providers.dart';

/// Pantalla de detalle de un portfolio con sus posiciones individuales,
/// rendimiento proyectado y opción de cancelación anticipada.
class PortfolioDetailPage extends ConsumerWidget {
  const PortfolioDetailPage({super.key, required this.portfolioId});

  final String portfolioId;

  static final _currencyFormat =
      NumberFormat.currency(locale: 'es_CO', symbol: '\$', decimalDigits: 0);
  static final _percentFormat =
      NumberFormat.percentPattern('es_CO')..maximumFractionDigits = 2;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final portfolioAsync = ref.watch(portfolioDetailProvider(portfolioId));

    return Scaffold(
      
      appBar: AppBar(
        
        foregroundColor: Theme.of(context).colorScheme.onSurface,
        title: Text('Detalle del Portfolio',
            style: TextStyle(fontWeight: FontWeight.bold)),
        elevation: 0,
      ),
      body: portfolioAsync.when(
        loading: () => const Center(
            child: CircularProgressIndicator(color: AppTheme.primaryColor)),
        error: (e, _) => Center(
            child: Text('Error al cargar el portfolio',
                style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.54)))),
        data: (portfolio) => _PortfolioDetailContent(
          portfolio: portfolio,
          currencyFormat: _currencyFormat,
          percentFormat: _percentFormat,
        ),
      ),
    );
  }
}

class _PortfolioDetailContent extends ConsumerWidget {
  const _PortfolioDetailContent({
    required this.portfolio,
    required this.currencyFormat,
    required this.percentFormat,
  });

  final InvestmentPortfolio portfolio;
  final NumberFormat currencyFormat;
  final NumberFormat percentFormat;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isActive = portfolio.estado == 'ACTIVE';

    return CustomScrollView(
      slivers: [
        SliverToBoxAdapter(
          child: Column(
            children: [
              // ── Card resumen ─────────────────────────────────────────
              Container(
                margin: const EdgeInsets.all(16),
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(20),
                  color: Theme.of(context).colorScheme.surface,
                  border: Border.all(
                      color: AppTheme.primaryColor.withOpacity(0.3)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text('Total Invertido',
                            style: TextStyle(
                                color: Theme.of(context).colorScheme.onSurface.withOpacity(0.54), fontSize: 13)),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 10, vertical: 4),
                          decoration: BoxDecoration(
                            color: (isActive
                                    ? AppTheme.primaryColor
                                    : Theme.of(context).colorScheme.onSurface.withOpacity(0.24))
                                .withOpacity(0.15),
                            borderRadius: BorderRadius.circular(20),
                          ),
                          child: Text(portfolio.estadoLabel,
                              style: TextStyle(
                                  color: isActive
                                      ? AppTheme.primaryColor
                                      : Theme.of(context).colorScheme.onSurface.withOpacity(0.38),
                                  fontSize: 12,
                                  fontWeight: FontWeight.bold)),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      currencyFormat.format(portfolio.montoTotal),
                      style: TextStyle(
                          color: Theme.of(context).colorScheme.onSurface,
                          fontSize: 30,
                          fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 16),
                    Divider(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.12)),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(
                          child: _SummaryMetric(
                            label: 'Rendimiento',
                            value: currencyFormat.format(
                                portfolio.rendimientoTotalProyectado),
                            valueColor: AppTheme.primaryColor,
                            subValue: '+${(portfolio.rendimientoPercentage).toStringAsFixed(2)}%',
                          ),
                        ),
                        Container(
                            width: 1, height: 48, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.12)),
                        Expanded(
                          child: _SummaryMetric(
                            label: 'Total al vencer',
                            value: currencyFormat
                                .format(portfolio.totalAlVencer),
                            valueColor: Theme.of(context).colorScheme.onSurface,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Icon(Icons.auto_graph,
                            size: 14, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.38)),
                        const SizedBox(width: 6),
                        Text(
                          'Estrategia: ${portfolio.estrategiaLabel} · ${portfolio.posiciones.length} posiciones',
                          style: TextStyle(
                              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.38), fontSize: 12),
                        ),
                      ],
                    ),
                  ],
                ),
              ),

              // ── Botón cancelar (solo si está ACTIVE) ─────────────────
              if (isActive)
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: OutlinedButton.icon(
                    onPressed: () =>
                        _confirmCancel(context, ref, portfolio.id),
                    icon: Icon(Icons.cancel_outlined,
                        color: Colors.redAccent),
                    label: Text('Cancelar Portfolio',
                        style: TextStyle(color: Colors.redAccent)),
                    style: OutlinedButton.styleFrom(
                      minimumSize: const Size(double.infinity, 48),
                      side: const BorderSide(color: Colors.redAccent),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12)),
                    ),
                  ),
                ),

              const SizedBox(height: 24),

              // ── Encabezado de posiciones ──────────────────────────────
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Row(
                  children: [
                    const Icon(Icons.pie_chart_outline,
                        color: AppTheme.primaryColor, size: 18),
                    const SizedBox(width: 8),
                    Text('Posiciones',
                        style: TextStyle(
                            color: Theme.of(context).colorScheme.onSurface,
                            fontSize: 17,
                            fontWeight: FontWeight.bold)),
                  ],
                ),
              ),
              const SizedBox(height: 12),
            ],
          ),
        ),

        // ── Lista de posiciones ───────────────────────────────────────
        SliverList(
          delegate: SliverChildBuilderDelegate(
            (context, i) => _PositionCard(
                position: portfolio.posiciones[i],
                currencyFormat: currencyFormat),
            childCount: portfolio.posiciones.length,
          ),
        ),
        const SliverToBoxAdapter(child: SizedBox(height: 32)),
      ],
    );
  }

  Future<void> _confirmCancel(
      BuildContext context, WidgetRef ref, String portfolioId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        
        title: Text('¿Cancelar portfolio?',
            style: TextStyle(color: Theme.of(context).colorScheme.onSurface)),
        content: Text(
          'Solo se devolverá el capital invertido, sin los rendimientos proyectados.',
          style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: Text('No',
                  style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.54)))),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, true),
            style: ElevatedButton.styleFrom(
                backgroundColor: Colors.redAccent),
            child: Text('Sí, cancelar'),
          ),
        ],
      ),
    );

    if (confirmed == true && context.mounted) {
      try {
        await ref
            .read(investmentPortfoliosProvider.notifier)
            .cancelPortfolio(portfolioId);
        if (context.mounted) {
          Navigator.of(context).pop();
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
                content: Text('Portfolio cancelado. Capital devuelto a tu saldo.'),
                backgroundColor: AppTheme.primaryColor),
          );
        }
      } catch (e) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text('Error: $e')));
        }
      }
    }
  }
}

class _SummaryMetric extends StatelessWidget {
  const _SummaryMetric(
      {required this.label,
      required this.value,
      required this.valueColor,
      this.subValue});

  final String label;
  final String value;
  final Color valueColor;
  final String? subValue;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label,
              style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.54), fontSize: 12)),
          const SizedBox(height: 2),
          Text(value,
              style: TextStyle(
                  color: valueColor,
                  fontSize: 18,
                  fontWeight: FontWeight.bold)),
          if (subValue != null)
            Text(subValue!,
                style: TextStyle(
                    color: valueColor.withOpacity(0.7), fontSize: 12)),
        ],
      ),
    );
  }
}

class _PositionCard extends StatelessWidget {
  const _PositionCard(
      {required this.position, required this.currencyFormat});

  final InvestmentPosition position;
  final NumberFormat currencyFormat;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(14),
        color: Theme.of(context).scaffoldBackgroundColor,
        border: Border.all(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.07)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(position.instrumentNombre,
                    style: TextStyle(
                        color: Theme.of(context).colorScheme.onSurface,
                        fontWeight: FontWeight.bold,
                        fontSize: 14)),
              ),
              Text(
                currencyFormat.format(position.montoInvertido),
                style: TextStyle(
                    color: AppTheme.primaryColor,
                    fontWeight: FontWeight.bold,
                    fontSize: 15),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              _MiniChip(label: '${(position.tasaAplicada * 100).toStringAsFixed(2)}% E.A.'),
              const SizedBox(width: 8),
              _MiniChip(label: '${position.plazoDias} días'),
              const SizedBox(width: 8),
              _MiniChip(label: 'Vence: ${position.fechaVencimiento}'),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            '+ ${currencyFormat.format(position.rendimientoProyectado)} en rendimiento',
            style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.54), fontSize: 12),
          ),
        ],
      ),
    );
  }
}

class _MiniChip extends StatelessWidget {
  const _MiniChip({required this.label});
  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.onSurface.withOpacity(0.06),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(label,
          style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.38), fontSize: 11)),
    );
  }
}
