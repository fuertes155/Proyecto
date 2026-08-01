import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/investment_remote_datasource.dart';
import '../../data/models/investment_models.dart';

/// Resumen agregado (sin identidad de otros socios) de en qué está
/// trabajando el capital que el usuario ha depositado, según el motor de
/// distribución P2P.
final investmentSummaryProvider =
    FutureProvider<InvestmentPortfolioSummary>((ref) {
  return ref.watch(investmentRemoteDataSourceProvider).getMySummary();
});
