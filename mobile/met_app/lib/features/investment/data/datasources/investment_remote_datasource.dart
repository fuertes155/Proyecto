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

  Future<List<InvestmentBreakdownItem>> getMyBreakdown() async {
    final response = await _dio.get('/v1/investments/my-breakdown');
    final list = response.data as List<dynamic>;
    return list
        .map((e) => InvestmentBreakdownItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
