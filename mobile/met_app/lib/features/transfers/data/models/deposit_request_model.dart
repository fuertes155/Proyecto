class DepositRequestModel {
  DepositRequestModel({
    required this.amount,
    required this.method,
    this.reference,
  });

  final double amount;
  final String method;
  final String? reference;

  Map<String, dynamic> toJson() => {
        'amount': amount,
        'method': method,
        if (reference != null) 'reference': reference,
      };
}
