import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/admin_repository.dart';

/// Alertas SARLAFT abiertas. Va aparte de [complianceAlertsProvider] porque
/// aquel sigue el filtro elegido en la pantalla de cumplimiento, y el inicio
/// siempre debe contar las abiertas.
final openAlertsCountProvider = FutureProvider<int>((ref) async {
  final data = await ref.watch(adminRepositoryProvider).getComplianceAlerts(status: 'OPEN');
  final total = data['total'];
  if (total is int) return total;
  return (data['items'] as List? ?? const []).length;
});
