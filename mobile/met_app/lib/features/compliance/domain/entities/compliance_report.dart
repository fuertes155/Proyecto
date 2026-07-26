/// Entidad de dominio que representa un reporte regulatorio o estado de cumplimiento.
class ComplianceReport {
  final String id;
  final String entityCode; // Ej. SFC, SES
  final String status;
  final DateTime generatedAt;
  final String downloadUrl;

  const ComplianceReport({
    required this.id,
    required this.entityCode,
    required this.status,
    required this.generatedAt,
    required this.downloadUrl,
  });
}
