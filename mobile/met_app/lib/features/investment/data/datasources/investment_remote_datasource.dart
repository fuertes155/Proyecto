import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client_provider.dart';
import '../models/investment_models.dart';

final investmentRemoteDataSourceProvider =
    Provider<InvestmentRemoteDataSource>((ref) {
  return InvestmentRemoteDataSource(ref.watch(apiClientProvider));
});

class InvestmentRemoteDataSource {
  InvestmentRemoteDataSource(this._dio);

  final Dio _dio;

  Future<InvestmentPortfolioSummary> getMySummary() async {
    final response = await _dio.get('/v1/investments/my-summary');
    return InvestmentPortfolioSummary.fromJson(response.data as Map<String, dynamic>);
  }
}
