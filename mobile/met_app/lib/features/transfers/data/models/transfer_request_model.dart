class TransferRequestModel {
  TransferRequestModel({
    required this.destinationAccountId,
    required this.amount,
    required this.pin,
    this.concept,
  });

  final String destinationAccountId;
  final double amount;
  final String pin;
  final String? concept;

  Map<String, dynamic> toJson() {
    return {
      'destinationAccountId': destinationAccountId,
      'amount': amount,
      'pin': pin,
      if (concept != null && concept!.isNotEmpty) 'concept': concept,
    };
  }
}
