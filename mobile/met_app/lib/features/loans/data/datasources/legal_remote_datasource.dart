import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client_provider.dart';

final legalRemoteDataSourceProvider = Provider<LegalRemoteDataSource>((ref) {
  return LegalRemoteDataSource(ref.watch(apiClientProvider));
});

class LegalRemoteDataSource {
  LegalRemoteDataSource(this._dio);
  final Dio _dio;

  /// Retorna un transactionId
  Future<String> requestSignature() async {
    final response = await _dio.post('/v1/legal/mandate/request-signature');
    return response.data['transactionId'] as String;
  }

  Future<Map<String, dynamic>> confirmSignature(String transactionId, String otp) async {
    final response = await _dio.post('/v1/legal/mandate/confirm-signature', data: {
      'transactionId': transactionId,
      'otpCode': otp,
    });
    return response.data as Map<String, dynamic>;
  }
}
