import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/investment_remote_datasource.dart';
import '../../data/models/investment_models.dart';

// ── Instrumentos disponibles ───────────────────────────────────────────────

final investmentInstrumentsProvider =
    FutureProvider<List<InvestmentInstrument>>((ref) {
  return ref.watch(investmentRemoteDataSourceProvider).listInstruments();
});

// ── Portfolios del usuario ─────────────────────────────────────────────────

final investmentPortfoliosProvider =
    StateNotifierProvider<InvestmentPortfoliosNotifier,
        AsyncValue<List<InvestmentPortfolio>>>((ref) {
  return InvestmentPortfoliosNotifier(
      ref.watch(investmentRemoteDataSourceProvider));
});

class InvestmentPortfoliosNotifier
    extends StateNotifier<AsyncValue<List<InvestmentPortfolio>>> {
  InvestmentPortfoliosNotifier(this._dataSource)
      : super(const AsyncValue.loading());

  final InvestmentRemoteDataSource _dataSource;

  Future<void> load() async {
    state = const AsyncValue.loading();
    try {
      final portfolios = await _dataSource.listPortfolios();
      state = AsyncValue.data(portfolios);
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<InvestmentPortfolio> createPortfolio(
      CreatePortfolioRequest request) async {
    final portfolio = await _dataSource.createPortfolio(request);
    await load();
    return portfolio;
  }

  Future<void> cancelPortfolio(String portfolioId) async {
    await _dataSource.cancelPortfolio(portfolioId);
    await load();
  }
}

// ── Detalle de portfolio ───────────────────────────────────────────────────

final portfolioDetailProvider =
    FutureProvider.family<InvestmentPortfolio, String>((ref, portfolioId) {
  return ref
      .watch(investmentRemoteDataSourceProvider)
      .getPortfolio(portfolioId);
});

// ── Historial de rendimientos ──────────────────────────────────────────────

final investmentReturnsProvider =
    FutureProvider<List<InvestmentReturn>>((ref) {
  return ref.watch(investmentRemoteDataSourceProvider).listReturns();
});

// ── Estado del formulario de creación ─────────────────────────────────────

final selectedStrategyProvider = StateProvider<String>((ref) => 'EQUAL');

final investmentAmountProvider = StateProvider<double>((ref) => 100000);
