import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../auth/presentation/providers/auth_provider.dart';
import '../../data/datasources/investment_remote_datasource.dart';
import '../../data/models/investment_models.dart';

/// Resumen agregado (sin identidad de otros socios) de en qué está
/// trabajando el capital que el usuario ha depositado, según el motor de
/// distribución P2P.
final investmentSummaryProvider =
    FutureProvider<InvestmentPortfolioSummary>((ref) {
  ref.watch(sessionUserIdProvider); // se descarta al cambiar de socio
  return ref.watch(investmentRemoteDataSourceProvider).getMySummary();
});
