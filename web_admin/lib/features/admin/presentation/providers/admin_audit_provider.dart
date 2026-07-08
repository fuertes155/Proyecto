import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/models/admin_models.dart';
import '../../data/repositories/admin_audit_repository.dart';
import '../../../../core/network/api_client_provider.dart';

final adminAuditRepositoryProvider = Provider((ref) {
  return AdminAuditRepository(ref.watch(apiClientProvider));
});

final auditLogsProvider = FutureProvider.autoDispose<List<AuditLogEntry>>((ref) async {
  final repository = ref.watch(adminAuditRepositoryProvider);
  return repository.getAuditLogs();
});
