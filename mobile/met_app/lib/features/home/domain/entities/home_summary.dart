/// Entidad de dominio que representa el resumen del home del usuario.
/// No tiene dependencias de frameworks externos (Flutter/JSON/HTTP).
class HomeSummary {
  final String userId;
  final double principalBalance;
  final double interestBalance;
  final String accountNumber;
  final String accountStatus;
  final List<RecentTransaction> recentTransactions;
  final double savingsTotal;
  final double investmentsTotal;
  final int pendingLoans;

  const HomeSummary({
    required this.userId,
    required this.principalBalance,
    required this.interestBalance,
    required this.accountNumber,
    required this.accountStatus,
    required this.recentTransactions,
    required this.savingsTotal,
    required this.investmentsTotal,
    required this.pendingLoans,
  });

  double get totalBalance => principalBalance + interestBalance;
}

/// Transacción reciente para el resumen del home.
class RecentTransaction {
  final String id;
  final String type;
  final double amount;
  final String description;
  final DateTime createdAt;
  final bool isCredit;

  const RecentTransaction({
    required this.id,
    required this.type,
    required this.amount,
    required this.description,
    required this.createdAt,
    required this.isCredit,
  });
}
