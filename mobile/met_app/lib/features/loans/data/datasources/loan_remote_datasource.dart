import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client.dart';
import '../models/loan_models.dart';

final loanRemoteDataSourceProvider = Provider<LoanRemoteDataSource>((ref) {
  return LoanRemoteDataSource(ref.watch(apiClientProvider));
});

class LoanRemoteDataSource {
  LoanRemoteDataSource(this._dio);
  final Dio _dio;

  Future<LoanSimulationResult> simulate(SimulateLoanRequest request) async {
    final response = await _dio.post('/v1/loans/simulate', data: request.toJson());
    return LoanSimulationResult.fromJson(response.data as Map<String, dynamic>);
  }

  Future<LoanApplication> submitApplication(SubmitLoanApplicationRequest request) async {
    final response = await _dio.post('/v1/loans/applications', data: request.toJson());
    return LoanApplication.fromJson(response.data as Map<String, dynamic>);
  }

  Future<List<LoanApplication>> listApplications() async {
    final response = await _dio.get('/v1/loans/applications');
    return (response.data as List)
        .map((e) => LoanApplication.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<LoanApplication> getApplication(String applicationId) async {
    final response = await _dio.get('/v1/loans/applications/$applicationId');
    return LoanApplication.fromJson(response.data as Map<String, dynamic>);
  }
}
