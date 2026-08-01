class MovementModel {
  MovementModel({
    required this.id,
    required this.type,
    required this.typeLabel,
    required this.concept,
    required this.amount,
    required this.isCredit,
    required this.status,
    required this.createdAt,
  });

  final String id;
  final String type;
  final String typeLabel;
  final String concept;
  final double amount;
  final bool isCredit;
  final String? status;
  final DateTime createdAt;

  factory MovementModel.fromJson(Map<String, dynamic> json) {
    return MovementModel(
      id: json['id'] as String,
      type: json['type'] as String,
      typeLabel: json['typeLabel'] as String,
      concept: json['concept'] as String? ?? '',
      amount: (json['amount'] as num).toDouble(),
      isCredit: json['isCredit'] as bool,
      status: json['status'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}
