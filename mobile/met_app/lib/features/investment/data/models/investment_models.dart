/// Resumen agregado de en qué está trabajando el capital del usuario dentro
/// del motor de distribución P2P: cuánto está activo generando rendimiento,
/// cuánto espera ser asignado y cuánto ya se recuperó. A propósito NO incluye
/// a qué socios concretos quedó emparejado cada fracción — esa identidad es
/// información sensible de otro socio y solo la ve un administrador.
class InvestmentPortfolioSummary {
  InvestmentPortfolioSummary({
    required this.totalInvested,
    required this.activeAmount,
    required this.availableAmount,
    required this.paidOffAmount,
    required this.returnedAmount,
    required this.loansFundedCount,
  });

  factory InvestmentPortfolioSummary.fromJson(Map<String, dynamic> json) {
    return InvestmentPortfolioSummary(
      totalInvested: (json['totalInvested'] as num).toDouble(),
      activeAmount: (json['activeAmount'] as num).toDouble(),
      availableAmount: (json['availableAmount'] as num).toDouble(),
      paidOffAmount: (json['paidOffAmount'] as num).toDouble(),
      returnedAmount: (json['returnedAmount'] as num).toDouble(),
      loansFundedCount: json['loansFundedCount'] as int,
    );
  }

  final double totalInvested;
  final double activeAmount;
  final double availableAmount;
  final double paidOffAmount;
  final double returnedAmount;
  final int loansFundedCount;
}
