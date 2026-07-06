class CoreAccountModel {
  CoreAccountModel({
    required this.id,
    required this.accountNumber,
    required this.balance,
    required this.status,
  });

  final String id;
  final String accountNumber;
  final double balance;
  final String status;

  factory CoreAccountModel.fromJson(Map<String, dynamic> json) {
    return CoreAccountModel(
      id: json['id'] as String,
      accountNumber: json['accountNumber'] as String,
      balance: (json['balance'] as num).toDouble(),
      status: json['status'] as String,
    );
  }
}
