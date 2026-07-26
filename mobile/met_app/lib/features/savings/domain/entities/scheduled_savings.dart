/// Entidad de dominio que representa una meta de ahorro programado.
class ScheduledSavings {
  final String id;
  final String title;
  final double targetAmount;
  final double currentAmount;
  final String frequency; // MONTHLY, WEEKLY
  final String status;

  const ScheduledSavings({
    required this.id,
    required this.title,
    required this.targetAmount,
    required this.currentAmount,
    required this.frequency,
    required this.status,
  });

  double get progressPercentage => (currentAmount / targetAmount).clamp(0.0, 1.0);
}
