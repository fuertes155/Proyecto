import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/investment_remote_datasource.dart';
import '../../data/models/investment_models.dart';

/// A qué deudores concretos (o al fondo de liquidez) está fondeando el
/// capital que el usuario ha depositado, según el motor de distribución P2P.
final investmentBreakdownProvider =
    FutureProvider<List<InvestmentBreakdownItem>>((ref) {
  return ref.watch(investmentRemoteDataSourceProvider).getMyBreakdown();
});
