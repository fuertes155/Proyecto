/// Entidad de dominio que representa un grupo solidario (ROSCA/Nativa).
class SolidarityGroup {
  final String id;
  final String name;
  final String groupCode;
  final double poolBalance;
  final int membersCount;
  final bool isOwner;

  const SolidarityGroup({
    required this.id,
    required this.name,
    required this.groupCode,
    required this.poolBalance,
    required this.membersCount,
    required this.isOwner,
  });
}
