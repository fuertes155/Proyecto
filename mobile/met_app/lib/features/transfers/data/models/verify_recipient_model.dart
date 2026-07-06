class VerifyRecipientModel {
  VerifyRecipientModel({
    required this.accountId,
    required this.ownerName,
  });

  final String accountId;
  final String ownerName;

  factory VerifyRecipientModel.fromJson(Map<String, dynamic> json) {
    return VerifyRecipientModel(
      accountId: json['accountId'] as String,
      ownerName: json['ownerName'] as String,
    );
  }
}
