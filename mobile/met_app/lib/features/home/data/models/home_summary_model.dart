import '../../domain/entities/home_summary.dart';

/// Modelo DTO que parsea el JSON de la API a la entidad de dominio.
class HomeSummaryModel extends HomeSummary {
  const HomeSummaryModel({
    required super.userId,
    required super.principalBalance,
    required super.interestBalance,
    required super.accountNumber,
    required super.accountStatus,
    required super.recentTransactions,
    required super.savingsTotal,
    required super.investmentsTotal,
    required super.pendingLoans,
  });

  factory HomeSummaryModel.fromJson(Map<String, dynamic> json) {
    return HomeSummaryModel(
      userId: json['userId'] ?? '',
      principalBalance: (json['principalBalance'] ?? 0).toDouble(),
      interestBalance: (json['interestBalance'] ?? 0).toDouble(),
      accountNumber: json['accountNumber'] ?? '',
      accountStatus: json['accountStatus'] ?? 'UNKNOWN',
      savingsTotal: (json['savingsTotal'] ?? 0).toDouble(),
      investmentsTotal: (json['investmentsTotal'] ?? 0).toDouble(),
      pendingLoans: json['pendingLoans'] ?? 0,
      recentTransactions: (json['recentTransactions'] as List<dynamic>?)
              ?.map((t) => RecentTransactionModel.fromJson(t))
              .toList() ??
          [],
    );
  }
}

class RecentTransactionModel extends RecentTransaction {
  const RecentTransactionModel({
    required super.id,
    required super.type,
    required super.amount,
    required super.description,
    required super.createdAt,
    required super.isCredit,
  });

  factory RecentTransactionModel.fromJson(Map<String, dynamic> json) {
    return RecentTransactionModel(
      id: json['id'] ?? '',
      type: json['type'] ?? 'UNKNOWN',
      amount: (json['amount'] ?? 0).toDouble(),
      description: json['description'] ?? '',
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'])
          : DateTime.now(),
      isCredit: json['isCredit'] ?? false,
    );
  }
}
