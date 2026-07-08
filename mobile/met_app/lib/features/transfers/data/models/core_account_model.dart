class CoreAccountModel {
  CoreAccountModel({
    required this.id,
    required this.accountNumber,
    required this.principalBalance,
    required this.interestBalance,
    required this.status,
  });

  final String id;
  final String accountNumber;
  final double principalBalance;
  final double interestBalance;
  final String status;

  factory CoreAccountModel.fromJson(Map<String, dynamic> json) {
    return CoreAccountModel(
      id: json['id'] as String,
      accountNumber: json['accountNumber'] as String,
      principalBalance: (json['principalBalance'] as num).toDouble(),
      interestBalance: (json['interestBalance'] as num).toDouble(),
      status: json['status'] as String,
    );
  }
}
