class BankModel {
  BankModel({
    required this.code,
    required this.name,
    required this.supportsPse,
    required this.supportsPayout,
  });

  final String code;
  final String name;
  final bool supportsPse;
  final bool supportsPayout;

  factory BankModel.fromJson(Map<String, dynamic> json) {
    return BankModel(
      code: json['code'] as String,
      name: json['name'] as String,
      supportsPse: json['supportsPse'] as bool? ?? false,
      supportsPayout: json['supportsPayout'] as bool? ?? false,
    );
  }
}
