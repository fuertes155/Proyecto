import '../../data/models/core_account_model.dart';
import '../../data/models/transfer_request_model.dart';
import '../../data/models/verify_recipient_model.dart';
import '../../data/models/deposit_request_model.dart';
import '../../data/models/recent_recipient_model.dart';

abstract class TransfersRepository {
  Future<CoreAccountModel> getMyAccount();
  Future<VerifyRecipientModel> verifyRecipient(String identifier);
  Future<List<RecentRecipientModel>> getRecentRecipients();
  Future<void> executeTransfer(TransferRequestModel request);
  Future<void> requestTransferOtp();
  Future<void> deposit(DepositRequestModel request);
  Future<String> generatePseLink(double amount, String returnUrl);
}
