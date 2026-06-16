import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/compliance_remote_datasource.dart';
import '../../data/models/compliance_models.dart';

final regulatoryReportsProvider = FutureProvider<List<RegulatoryReport>>((ref) {
  return ref.watch(complianceRemoteDataSourceProvider).listReports();
});

final reportTypesProvider = FutureProvider<List<ReportTypeInfo>>((ref) {
  return ref.watch(complianceRemoteDataSourceProvider).listReportTypes();
});
