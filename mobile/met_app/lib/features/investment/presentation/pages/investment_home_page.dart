import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../data/models/investment_models.dart';
import '../providers/investment_providers.dart';

/// Pantalla principal de inversiones.
/// Muestra instrumentos disponibles, portfolios activos y botón para invertir.
class InvestmentHomePage extends ConsumerStatefulWidget {
  const InvestmentHomePage({super.key});

  @override
  ConsumerState<InvestmentHomePage> createState() => _InvestmentHomePageState();
}

class _InvestmentHomePageState extends ConsumerState<InvestmentHomePage>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  final _currencyFormat =
      NumberFormat.currency(locale: 'es_CO', symbol: '\$', decimalDigits: 0);

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    Future.microtask(
        () => ref.read(investmentPortfoliosProvider.notifier).load());
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final cs = theme.colorScheme;

    return Scaffold(
      backgroundColor: const Color(0xFF0F1623),
      body: CustomScrollView(
        slivers: [
          // ── Header con gradiente ──────────────────────────────────────
          SliverAppBar(
            expandedHeight: 180,
            pinned: true,
            backgroundColor: const Color(0xFF0F1623),
            flexibleSpace: FlexibleSpaceBar(
              background: Container(
                decoration: const BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [Color(0xFF1A2E1A), Color(0xFF0F1623)],
                  ),
                ),
                padding: const EdgeInsets.fromLTRB(20, 80, 20, 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: const Color(0xFF53A835).withOpacity(0.2),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: const Icon(Icons.trending_up,
                              color: Color(0xFF53A835), size: 28),
                        ),
                        const SizedBox(width: 12),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('Mis Inversiones',
                                style: theme.textTheme.titleLarge?.copyWith(
                                    color: Colors.white,
                                    fontWeight: FontWeight.bold)),
                            Text('Haz crecer tu dinero',
                                style: theme.textTheme.bodyMedium?.copyWith(
                                    color: Colors.white54)),
                          ],
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            bottom: TabBar(
              controller: _tabController,
              labelColor: const Color(0xFF53A835),
              unselectedLabelColor: Colors.white38,
              indicatorColor: const Color(0xFF53A835),
              tabs: const [
                Tab(text: 'Instrumentos'),
                Tab(text: 'Mis Portfolios'),
              ],
            ),
          ),

          // ── Contenido de las tabs ─────────────────────────────────────
          SliverFillRemaining(
            child: TabBarView(
              controller: _tabController,
              children: [
                _InstrumentsTab(currencyFormat: _currencyFormat),
                _PortfoliosTab(currencyFormat: _currencyFormat),
              ],
            ),
          ),
        ],
      ),

      // ── FAB: Invertir ────────────────────────────────────────────────
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/investments/create'),
        backgroundColor: const Color(0xFF53A835),
        foregroundColor: Colors.white,
        icon: const Icon(Icons.add_circle_outline),
        label: const Text('Invertir',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
      ),
    );
  }
}

// ── Tab de Instrumentos ────────────────────────────────────────────────────

class _InstrumentsTab extends ConsumerWidget {
  const _InstrumentsTab({required this.currencyFormat});
  final NumberFormat currencyFormat;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final instrumentsAsync = ref.watch(investmentInstrumentsProvider);

    return instrumentsAsync.when(
      loading: () => const Center(
          child: CircularProgressIndicator(color: Color(0xFF53A835))),
      error: (e, _) => Center(
          child: Text('Error cargando instrumentos',
              style: TextStyle(color: Colors.white54))),
      data: (instruments) {
        if (instruments.isEmpty) {
          return const Center(
            child: Text('No hay instrumentos disponibles',
                style: TextStyle(color: Colors.white54)),
          );
        }
        return ListView.builder(
          padding: const EdgeInsets.all(16),
          itemCount: instruments.length,
          itemBuilder: (_, i) =>
              _InstrumentCard(instrument: instruments[i], currencyFormat: currencyFormat),
        );
      },
    );
  }
}

class _InstrumentCard extends StatelessWidget {
  const _InstrumentCard(
      {required this.instrument, required this.currencyFormat});
  final InvestmentInstrument instrument;
  final NumberFormat currencyFormat;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF1C2B1C), Color(0xFF1A2332)],
        ),
        border: Border.all(color: const Color(0xFF53A835).withOpacity(0.2)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(instrument.nombre,
                      style: const TextStyle(
                          color: Colors.white,
                          fontSize: 17,
                          fontWeight: FontWeight.bold)),
                ),
                // Badge de tasa
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(
                    color: const Color(0xFF53A835).withOpacity(0.15),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(
                        color: const Color(0xFF53A835).withOpacity(0.5)),
                  ),
                  child: Text(instrument.tasaLabel,
                      style: const TextStyle(
                          color: Color(0xFF53A835),
                          fontWeight: FontWeight.bold,
                          fontSize: 15)),
                ),
              ],
            ),
            if (instrument.descripcion != null) ...[
              const SizedBox(height: 8),
              Text(instrument.descripcion!,
                  style:
                      const TextStyle(color: Colors.white54, fontSize: 13)),
            ],
            const SizedBox(height: 12),
            Row(
              children: [
                _InfoChip(
                    icon: Icons.schedule,
                    label: instrument.plazoLabel),
                const SizedBox(width: 12),
                _InfoChip(
                    icon: Icons.monetization_on_outlined,
                    label: 'Mín: ${currencyFormat.format(instrument.montoMinimo)}'),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoChip extends StatelessWidget {
  const _InfoChip({required this.icon, required this.label});
  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 14, color: Colors.white38),
        const SizedBox(width: 4),
        Text(label,
            style: const TextStyle(color: Colors.white38, fontSize: 13)),
      ],
    );
  }
}

// ── Tab de Portfolios ──────────────────────────────────────────────────────

class _PortfoliosTab extends ConsumerWidget {
  const _PortfoliosTab({required this.currencyFormat});
  final NumberFormat currencyFormat;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final portfoliosAsync = ref.watch(investmentPortfoliosProvider);

    return portfoliosAsync.when(
      loading: () => const Center(
          child: CircularProgressIndicator(color: Color(0xFF53A835))),
      error: (e, _) => Center(
          child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.error_outline, color: Colors.white38, size: 48),
          const SizedBox(height: 12),
          Text('Error al cargar portfolios',
              style: const TextStyle(color: Colors.white54)),
          const SizedBox(height: 12),
          ElevatedButton(
            onPressed: () =>
                ref.read(investmentPortfoliosProvider.notifier).load(),
            child: const Text('Reintentar'),
          ),
        ],
      )),
      data: (portfolios) {
        if (portfolios.isEmpty) {
          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.account_balance_wallet_outlined,
                    size: 64, color: Colors.white24),
                const SizedBox(height: 16),
                const Text('Aún no has invertido',
                    style: TextStyle(color: Colors.white54, fontSize: 18)),
                const SizedBox(height: 8),
                const Text('Toca el botón "Invertir" para comenzar',
                    style: TextStyle(color: Colors.white38, fontSize: 14)),
              ],
            ),
          );
        }
        return ListView.builder(
          padding: const EdgeInsets.all(16),
          itemCount: portfolios.length,
          itemBuilder: (_, i) => _PortfolioCard(
              portfolio: portfolios[i], currencyFormat: currencyFormat),
        );
      },
    );
  }
}

class _PortfolioCard extends StatelessWidget {
  const _PortfolioCard(
      {required this.portfolio, required this.currencyFormat});
  final InvestmentPortfolio portfolio;
  final NumberFormat currencyFormat;

  @override
  Widget build(BuildContext context) {
    final isActive = portfolio.estado == 'ACTIVE';
    final accentColor =
        isActive ? const Color(0xFF53A835) : Colors.white24;

    return GestureDetector(
      onTap: () =>
          context.push('/investments/portfolio/${portfolio.id}'),
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(16),
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: isActive
                ? [const Color(0xFF1C2B1C), const Color(0xFF1A2332)]
                : [const Color(0xFF1A1A1A), const Color(0xFF1A1A1A)],
          ),
          border: Border.all(color: accentColor.withOpacity(0.3)),
        ),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    currencyFormat.format(portfolio.montoTotal),
                    style: const TextStyle(
                        color: Colors.white,
                        fontSize: 22,
                        fontWeight: FontWeight.bold),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: accentColor.withOpacity(0.15),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(portfolio.estadoLabel,
                        style: TextStyle(
                            color: accentColor,
                            fontSize: 12,
                            fontWeight: FontWeight.bold)),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  const Icon(Icons.show_chart,
                      size: 16, color: Color(0xFF53A835)),
                  const SizedBox(width: 4),
                  Text(
                    'Rendimiento proyectado: ${currencyFormat.format(portfolio.rendimientoTotalProyectado)}',
                    style: const TextStyle(
                        color: Color(0xFF53A835), fontSize: 14),
                  ),
                ],
              ),
              const SizedBox(height: 4),
              Text(
                '${portfolio.posiciones.length} posiciones · Estrategia: ${portfolio.estrategiaLabel}',
                style: const TextStyle(color: Colors.white38, fontSize: 12),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
