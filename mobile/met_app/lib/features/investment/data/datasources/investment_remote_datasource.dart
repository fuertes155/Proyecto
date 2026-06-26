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

  Future<List<InvestmentInstrument>> listInstruments() async {
    final response = await _dio.get('/v1/investments/instruments');
    final list = response.data as List<dynamic>;
    return list
        .map((e) => InvestmentInstrument.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<InvestmentPortfolio> createPortfolio(
      CreatePortfolioRequest request) async {
    final response =
        await _dio.post('/v1/investments/portfolio', data: request.toJson());
    return InvestmentPortfolio.fromJson(
        response.data as Map<String, dynamic>);
  }

  Future<List<InvestmentPortfolio>> listPortfolios() async {
    final response = await _dio.get('/v1/investments/portfolio');
    final list = response.data as List<dynamic>;
    return list
        .map((e) => InvestmentPortfolio.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<InvestmentPortfolio> getPortfolio(String portfolioId) async {
    final response =
        await _dio.get('/v1/investments/portfolio/$portfolioId');
    return InvestmentPortfolio.fromJson(
        response.data as Map<String, dynamic>);
  }

  Future<void> cancelPortfolio(String portfolioId) async {
    await _dio.delete('/v1/investments/portfolio/$portfolioId');
  }

  Future<List<InvestmentReturn>> listReturns() async {
    final response = await _dio.get('/v1/investments/returns');
    final list = response.data as List<dynamic>;
    return list
        .map((e) => InvestmentReturn.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
