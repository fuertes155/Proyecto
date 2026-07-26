import '../entities/investment_portfolio.dart';

abstract class InvestmentRepository {
  Future<List<InvestmentPortfolio>> getMyPortfolios();
  Future<InvestmentPortfolio> createPortfolio(double amount, String strategy);
}
