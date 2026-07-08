import '../../domain/repositories/transfers_repository.dart';
import '../datasources/transfers_remote_datasource.dart';
import '../models/core_account_model.dart';
import '../models/transfer_request_model.dart';
import '../models/deposit_request_model.dart';
import '../models/verify_recipient_model.dart';

class TransfersRepositoryImpl implements TransfersRepository {
  TransfersRepositoryImpl(this._remoteDataSource);

  final TransfersRemoteDataSource _remoteDataSource;

  @override
  Future<CoreAccountModel> getMyAccount() {
    return _remoteDataSource.getMyAccount();
  }

  @override
  Future<VerifyRecipientModel> verifyRecipient(String identifier) {
    return _remoteDataSource.verifyRecipient(identifier);
  }

  @override
  Future<void> executeTransfer(TransferRequestModel request) async {
    return _remoteDataSource.executeTransfer(request);
  }

  @override
  Future<void> requestTransferOtp() async {
    return _remoteDataSource.requestTransferOtp();
  }

  @override
  Future<void> deposit(DepositRequestModel request) async {
    return _remoteDataSource.deposit(request);
  }
}
