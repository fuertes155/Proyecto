import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client_provider.dart';
import '../models/solidarity_models.dart';

final solidarityRemoteDataSourceProvider =
    Provider<SolidarityRemoteDataSource>((ref) {
  return SolidarityRemoteDataSource(ref.watch(apiClientProvider));
});

class SolidarityRemoteDataSource {
  SolidarityRemoteDataSource(this._dio);
  final Dio _dio;

  Future<List<SolidarityGroup>> listGroups() async {
    final response = await _dio.get('/v1/solidarity/groups');
    return (response.data as List)
        .map((e) => SolidarityGroup.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<SolidarityGroup> getGroup(String groupId) async {
    final response = await _dio.get('/v1/solidarity/groups/$groupId');
    return SolidarityGroup.fromJson(response.data as Map<String, dynamic>);
  }

  Future<SolidarityGroup> createGroup(
      CreateSolidarityGroupRequest request) async {
    final response =
        await _dio.post('/v1/solidarity/groups', data: request.toJson());
    return SolidarityGroup.fromJson(response.data as Map<String, dynamic>);
  }

  Future<SolidarityGroup> joinGroup(JoinSolidarityGroupRequest request) async {
    final response =
        await _dio.post('/v1/solidarity/groups/join', data: request.toJson());
    return SolidarityGroup.fromJson(response.data as Map<String, dynamic>);
  }

  Future<SolidarityGroup> contribute(
      String groupId, ContributeToPoolRequest request) async {
    final response = await _dio.post(
        '/v1/solidarity/groups/$groupId/contributions',
        data: request.toJson());
    return SolidarityGroup.fromJson(response.data as Map<String, dynamic>);
  }

  Future<MicroLoan> requestLoan(
      String groupId, RequestMicroLoanRequest request) async {
    final response = await _dio.post('/v1/solidarity/groups/$groupId/loans',
        data: request.toJson());
    return MicroLoan.fromJson(response.data as Map<String, dynamic>);
  }

  Future<MicroLoan> reviewLoan(
      String groupId, String loanId, ReviewMicroLoanRequest request) async {
    final response = await _dio.post(
      '/v1/solidarity/groups/$groupId/loans/$loanId/review',
      data: request.toJson(),
    );
    return MicroLoan.fromJson(response.data as Map<String, dynamic>);
  }

  Future<List<MicroLoan>> listLoans(String groupId) async {
    final response = await _dio.get('/v1/solidarity/groups/$groupId/loans');
    return (response.data as List)
        .map((e) => MicroLoan.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<LoanInstallment>> listInstallments(
      String groupId, String loanId) async {
    final response = await _dio
        .get('/v1/solidarity/groups/$groupId/loans/$loanId/installments');
    return (response.data as List)
        .map((e) => LoanInstallment.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> payInstallment(
      String groupId, String loanId, String installmentId) async {
    await _dio.post(
        '/v1/solidarity/groups/$groupId/loans/$loanId/installments/$installmentId/pay');
  }

  Future<List<PoolTransaction>> listTransactions(String groupId) async {
    final response =
        await _dio.get('/v1/solidarity/groups/$groupId/transactions');
    return (response.data as List)
        .map((e) => PoolTransaction.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
