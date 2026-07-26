/// Entidad de dominio que representa un portafolio de inversión (CDT o Fondo Mutuo).
class InvestmentPortfolio {
  final String id;
  final double investedAmount;
  final double currentYield;
  final String strategy;
  final DateTime maturityDate;

  const InvestmentPortfolio({
    required this.id,
    required this.investedAmount,
    required this.currentYield,
    required this.strategy,
    required this.maturityDate,
  });

  double get totalValue => investedAmount + currentYield;
}
