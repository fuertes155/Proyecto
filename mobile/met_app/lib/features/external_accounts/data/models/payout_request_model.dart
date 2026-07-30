class PayoutRequestModel {
  PayoutRequestModel({
    required this.externalBankAccountId,
    required this.amount,
    required this.pin,
    required this.idempotencyKey,
    this.concept,
    this.otp,
  });

  final String externalBankAccountId;
  final double amount;
  final String pin;
  final String idempotencyKey;
  final String? concept;
  final String? otp;

  Map<String, dynamic> toJson() {
    return {
      'externalBankAccountId': externalBankAccountId,
      'amount': amount,
      'pin': pin,
      'idempotencyKey': idempotencyKey,
      if (concept != null && concept!.isNotEmpty) 'concept': concept,
      if (otp != null) 'otp': otp,
    };
  }
}
