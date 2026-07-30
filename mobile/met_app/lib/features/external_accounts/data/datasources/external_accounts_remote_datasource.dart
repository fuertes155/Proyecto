import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client_provider.dart';
import '../../../../core/security/rsa_encryption_service.dart';
import '../models/bank_model.dart';
import '../models/external_bank_account_model.dart';
import '../models/payout_request_model.dart';
import '../models/register_external_bank_account_request.dart';

final externalAccountsRemoteDataSourceProvider = Provider<ExternalAccountsRemoteDataSource>((ref) {
  return ExternalAccountsRemoteDataSource(
    ref.watch(apiClientProvider),
    ref.watch(rsaEncryptionServiceProvider),
  );
});

class ExternalAccountsRemoteDataSource {
  ExternalAccountsRemoteDataSource(this._dio, this._rsaEncryptionService);

  final Dio _dio;
  final RsaEncryptionService _rsaEncryptionService;

  /// [type] es 'PAYOUT' o 'PSE' — ver ListBanksUseCase.CatalogType en el backend.
  Future<List<BankModel>> listBanks(String type) async {
    final response = await _dio.get('/v1/banks', queryParameters: {'type': type});
    return (response.data as List<dynamic>)
        .map((e) => BankModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<ExternalBankAccountModel>> listMyAccounts() async {
    final response = await _dio.get('/v1/accounts/external-bank-accounts');
    return (response.data as List<dynamic>)
        .map((e) => ExternalBankAccountModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> registerAccount(RegisterExternalBankAccountRequest request) async {
    await _dio.post('/v1/accounts/external-bank-accounts', data: request.toJson());
  }

  /// Reenvía el micro-depósito de verificación de titularidad.
  Future<void> resendVerification(String accountId) async {
    await _dio.post('/v1/accounts/external-bank-accounts/$accountId/verify/resend');
  }

  /// Confirma el monto exacto del micro-depósito visto en el extracto bancario.
  Future<void> confirmVerification(String accountId, int amount) async {
    await _dio.post('/v1/accounts/external-bank-accounts/$accountId/verify', data: {
      'amount': amount,
    });
  }

  /// El OTP en el backend está atado al usuario, no al tipo de operación —
  /// se reutiliza el mismo endpoint que ya usa el flujo de transferencias.
  Future<void> requestPayoutOtp() async {
    await _dio.post('/v1/accounts/transactions/transfer/otp/request');
  }

  Future<void> executePayout(PayoutRequestModel request) async {
    final payload = request.toJson();
    payload['pin'] = await _rsaEncryptionService.encryptPin(payload['pin'] as String);
    await _dio.post('/v1/accounts/transactions/payout', data: payload);
  }
}
