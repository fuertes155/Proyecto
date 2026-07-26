import '../entities/compliance_report.dart';

abstract class ComplianceRepository {
  Future<List<ComplianceReport>> getReports();
  Future<void> generateUiafReport();
}
