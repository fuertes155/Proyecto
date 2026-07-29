class RecentRecipientModel {
  RecentRecipientModel({
    required this.accountId,
    required this.accountNumber,
    required this.ownerName,
  });

  final String accountId;
  final String accountNumber;
  final String ownerName;

  factory RecentRecipientModel.fromJson(Map<String, dynamic> json) {
    return RecentRecipientModel(
      accountId: json['accountId'] as String,
      accountNumber: json['accountNumber'] as String,
      ownerName: json['ownerName'] as String,
    );
  }
}
