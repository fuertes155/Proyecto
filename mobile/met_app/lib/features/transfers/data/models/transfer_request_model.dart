class TransferRequestModel {
  TransferRequestModel({
    required this.destinationAccountId,
    required this.amount,
    required this.pin,
    this.concept,
    this.otp,
  });

  final String destinationAccountId;
  final double amount;
  final String pin;
  final String? concept;
  final String? otp;

  Map<String, dynamic> toJson() {
    return {
      'destinationAccountId': destinationAccountId,
      'amount': amount,
      'pin': pin,
      if (concept != null && concept!.isNotEmpty) 'concept': concept,
      if (otp != null) 'otp': otp,
    };
  }
}
