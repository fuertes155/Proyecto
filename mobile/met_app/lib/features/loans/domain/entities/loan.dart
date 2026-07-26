/// Entidad de dominio que representa una solicitud de préstamo o crédito activo.
class Loan {
  final String id;
  final double amount;
  final int termsInMonths;
  final double interestRate;
  final String status;
  final DateTime createdAt;

  const Loan({
    required this.id,
    required this.amount,
    required this.termsInMonths,
    required this.interestRate,
    required this.status,
    required this.createdAt,
  });
}
