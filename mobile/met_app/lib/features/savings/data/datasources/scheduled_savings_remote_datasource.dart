import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client_provider.dart';
import '../models/scheduled_savings_models.dart';

final scheduledSavingsRemoteDataSourceProvider =
    Provider<ScheduledSavingsRemoteDataSource>((ref) {
  return ScheduledSavingsRemoteDataSource(ref.watch(apiClientProvider));
});

class ScheduledSavingsRemoteDataSource {
  ScheduledSavingsRemoteDataSource(this._dio);

  final Dio _dio;

  Future<List<ScheduledSavingsAccount>> listAccounts() async {
    final response = await _dio.get('/v1/savings/scheduled');
    final list = response.data as List<dynamic>;
    return list
        .map((e) => ScheduledSavingsAccount.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<ScheduledSavingsAccount> getAccount(String accountId) async {
    final response = await _dio.get('/v1/savings/scheduled/$accountId');
    return ScheduledSavingsAccount.fromJson(
        response.data as Map<String, dynamic>);
  }

  Future<ScheduledSavingsAccount> createAccount(
      CreateScheduledSavingsRequest request) async {
    final response =
        await _dio.post('/v1/savings/scheduled', data: request.toJson());
    return ScheduledSavingsAccount.fromJson(
        response.data as Map<String, dynamic>);
  }

  Future<ScheduledSavingsAccount> updateAccount(
    String accountId,
    UpdateScheduledSavingsRequest request,
  ) async {
    final response = await _dio.patch(
      '/v1/savings/scheduled/$accountId',
      data: request.toJson(),
    );
    return ScheduledSavingsAccount.fromJson(
        response.data as Map<String, dynamic>);
  }

  Future<List<ContributionItem>> getContributions(String accountId) async {
    final response =
        await _dio.get('/v1/savings/scheduled/$accountId/contributions');
    final list = response.data as List<dynamic>;
    return list
        .map((e) => ContributionItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
