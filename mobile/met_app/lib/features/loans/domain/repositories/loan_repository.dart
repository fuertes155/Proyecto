import '../entities/loan.dart';

/// Contrato para el repositorio de préstamos.
abstract class LoanRepository {
  /// Obtiene la lista de préstamos y solicitudes del usuario.
  Future<List<Loan>> getMyLoans();

  /// Solicita un nuevo préstamo personal.
  Future<Loan> applyForLoan(double amount, int terms);

  /// Simula un préstamo para ver cuotas estimadas.
  Future<Map<String, dynamic>> simulateLoan(double amount, int terms);
}
