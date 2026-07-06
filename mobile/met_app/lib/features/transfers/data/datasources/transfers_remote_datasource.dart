import 'package:dio/dio.dart';
import '../models/core_account_model.dart';
import '../models/transfer_request_model.dart';
import '../models/deposit_request_model.dart';
import '../models/verify_recipient_model.dart';

class TransfersRemoteDataSource {
  TransfersRemoteDataSource(this._dio);

  final Dio _dio;

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
    await _dio.post('/v1/accounts/transactions/transfer', data: request.toJson());
  }

  Future<void> deposit(DepositRequestModel request) async {
    await _dio.post('/v1/accounts/deposit', data: request.toJson());
  }
}
