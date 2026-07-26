import '../entities/scheduled_savings.dart';

/// Contrato para el repositorio de ahorros programados.
abstract class SavingsRepository {
  /// Obtiene los ahorros activos del usuario.
  Future<List<ScheduledSavings>> getMySavings();

  /// Crea una nueva meta de ahorro.
  Future<ScheduledSavings> createSavingsGoal(
    String title,
    double targetAmount,
    String frequency,
    int dayOfMonth,
  );
}
