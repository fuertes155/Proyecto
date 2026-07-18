import 'package:dio/dio.dart';
import '../../../../core/network/api_client_provider.dart';
import '../../../../core/security/rsa_encryption_service.dart';
import '../models/core_account_model.dart';
import '../models/transfer_request_model.dart';
import '../models/deposit_request_model.dart';
import '../models/verify_recipient_model.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final transfersRemoteDataSourceProvider = Provider<TransfersRemoteDataSource>((ref) {
  return TransfersRemoteDataSource(
    ref.watch(apiClientProvider),
    ref.watch(rsaEncryptionServiceProvider),
  );
});

class TransfersRemoteDataSource {
  TransfersRemoteDataSource(this._dio, this._rsaEncryptionService);

  final Dio _dio;
  final RsaEncryptionService _rsaEncryptionService;

  Future<CoreAccountModel> getMyAccount() async {
    final response = await _dio.get('/v1/accounts/me');
    return CoreAccountModel.fromJson(response.data);
  }

  Future<VerifyRecipientModel> verifyRecipient(String identifier) async {
    final response = await _dio.get('/v1/accounts/verify', queryParameters: {
      'identifier': identifier,
    });
    return VerifyRecipientModel.fromJson(response.data);
  }

  Future<void> executeTransfer(TransferRequestModel request) async {
    final payload = request.toJson();
    if (payload['pin'] != null) {
      payload['pin'] = await _rsaEncryptionService.encryptPin(payload['pin'] as String);
    }
    await _dio.post('/v1/accounts/transactions/transfer', data: payload);
  }

  Future<void> requestTransferOtp() async {
    await _dio.post('/v1/accounts/transactions/transfer/otp/request');
  }

  Future<void> deposit(DepositRequestModel request) async {
    await _dio.post('/v1/accounts/deposit', data: request.toJson());
  }

  Future<String> generatePseLink(double amount, String returnUrl) async {
    final response = await _dio.post('/v1/accounts/deposit-pse', data: {
      'amount': amount,
      'returnUrl': returnUrl,
    });
    final url = response.data['paymentUrl'] as String;
    if (url.startsWith('http')) return url;
    // baseUrl = 'http://localhost:8080/api', url starts with '/v1'
    final base = _dio.options.baseUrl.endsWith('/')
        ? _dio.options.baseUrl.substring(0, _dio.options.baseUrl.length - 1)
        : _dio.options.baseUrl;
    return '$base$url';
  }
}
