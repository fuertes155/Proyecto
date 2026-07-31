/// Una posición real del motor de distribución P2P: a qué deudor (o al
/// fondo de liquidez, si aún no fue emparejada) quedó fraccionado el
/// capital depositado por el usuario.
class InvestmentBreakdownItem {
  InvestmentBreakdownItem({
    required this.fractionId,
    required this.borrowerName,
    this.loanId,
    required this.amount,
    required this.status,
    this.matchedAt,
  });

  factory InvestmentBreakdownItem.fromJson(Map<String, dynamic> json) {
    return InvestmentBreakdownItem(
      fractionId: json['fractionId'] as String,
      borrowerName: json['borrowerName'] as String,
      loanId: json['loanId'] as String?,
      amount: (json['amount'] as num).toDouble(),
      status: json['status'] as String,
      matchedAt: json['matchedAt'] as String?,
    );
  }

  final String fractionId;
  final String borrowerName;
  final String? loanId;
  final double amount;

  /// DISPONIBLE | ACTIVO | PAGADO | DEVUELTO
  final String status;
  final String? matchedAt;

  String get statusLabel => switch (status) {
        'DISPONIBLE' => 'Disponible',
        'ACTIVO' => 'Activo',
        'PAGADO' => 'Pagado',
        'DEVUELTO' => 'Devuelto',
        _ => status,
      };

  bool get isLiquidityFund => status == 'DISPONIBLE';
}
