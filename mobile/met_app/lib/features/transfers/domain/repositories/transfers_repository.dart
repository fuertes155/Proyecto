import '../../data/models/core_account_model.dart';
import '../../data/models/transfer_request_model.dart';
import '../../data/models/verify_recipient_model.dart';

abstract class TransfersRepository {
  Future<CoreAccountModel> getMyAccount();
  Future<VerifyRecipientModel> verifyRecipient(String identifier);
  Future<void> executeTransfer(TransferRequestModel request);
}
