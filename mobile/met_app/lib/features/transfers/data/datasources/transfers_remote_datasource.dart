import 'package:dio/dio.dart';
import '../../../../core/network/api_client_provider.dart';
import '../../../../core/security/rsa_encryption_service.dart';
import '../models/core_account_model.dart';
import '../models/transfer_request_model.dart';
import '../models/deposit_request_model.dart';
import '../models/verify_recipient_model.dart';
import '../models/recent_recipient_model.dart';
import '../models/movement_model.dart';
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

  Future<List<RecentRecipientModel>> getRecentRecipients() async {
    final response = await _dio.get('/v1/accounts/recent-recipients');
    return (response.data as List<dynamic>)
        .map((e) => RecentRecipientModel.fromJson(e as Map<String, dynamic>))
        .toList();
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

  Future<List<MovementModel>> getMovements() async {
    final response = await _dio.get('/v1/accounts/movements');
    return (response.data as List<dynamic>)
        .map((e) => MovementModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<String> getStatementCsv({required int year, required int month}) async {
    final response = await _dio.get<String>(
      '/v1/accounts/statement',
      queryParameters: {'year': year, 'month': month},
      options: Options(responseType: ResponseType.plain),
    );
    return response.data ?? '';
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

  /// Depósito PSE nativo: el usuario ya eligió el banco dentro de la app
  /// (selector alimentado por GET /v1/banks?type=PSE), así que aquí se crea
  /// la transacción directamente en Wompi en vez de redirigir al checkout
  /// hosteado donde tendría que elegir el banco de nuevo.
  Future<String> createNativePseDeposit(double amount, String bankCode, String returnUrl) async {
    final response = await _dio.post('/v1/accounts/deposit-pse/native', data: {
      'amount': amount,
      'bankCode': bankCode,
      'returnUrl': returnUrl,
    });
    return response.data['asyncPaymentUrl'] as String;
  }
}
