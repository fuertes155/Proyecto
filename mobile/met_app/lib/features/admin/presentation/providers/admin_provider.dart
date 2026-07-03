import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/admin_models.dart';
import '../../data/repositories/admin_repository.dart';

// ── Auth State ────────────────────────────────────────────────────────────────
final adminAuthProvider =
    StateNotifierProvider<AdminAuthNotifier, AsyncValue<AdminAuthResponse?>>((ref) {
  return AdminAuthNotifier(ref.watch(adminRepositoryProvider));
});

class AdminAuthNotifier extends StateNotifier<AsyncValue<AdminAuthResponse?>> {
  AdminAuthNotifier(this._repo) : super(const AsyncValue.data(null));

  final AdminRepository _repo;

  Future<void> login(String username, String password) async {
    state = const AsyncValue.loading();
    try {
      final response = await _repo.login(AdminLoginRequest(username: username, password: password));
      state = AsyncValue.data(response);
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<void> logout() async {
    await _repo.logout();
    state = const AsyncValue.data(null);
  }
}

// ── Operation Limits ─────────────────────────────────────────────────────────
final operationLimitsProvider =
    StateNotifierProvider<OperationLimitsNotifier, AsyncValue<List<OperationLimit>>>((ref) {
  return OperationLimitsNotifier(ref.watch(adminRepositoryProvider));
});

class OperationLimitsNotifier extends StateNotifier<AsyncValue<List<OperationLimit>>> {
  OperationLimitsNotifier(this._repo) : super(const AsyncValue.loading()) {
    load();
  }
  final AdminRepository _repo;

  Future<void> load() async {
    state = const AsyncValue.loading();
    try {
      state = AsyncValue.data(await _repo.getOperationLimits());
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<void> update(String tipo, int diario, int porTx) async {
    await _repo.updateLimit(tipo, diario, porTx);
    await load();
  }
}

// ── Risk Rules ────────────────────────────────────────────────────────────────
final riskRulesProvider =
    StateNotifierProvider<RiskRulesNotifier, AsyncValue<List<RiskRule>>>((ref) {
  return RiskRulesNotifier(ref.watch(adminRepositoryProvider));
});

class RiskRulesNotifier extends StateNotifier<AsyncValue<List<RiskRule>>> {
  RiskRulesNotifier(this._repo) : super(const AsyncValue.loading()) {
    load();
  }
  final AdminRepository _repo;

  Future<void> load() async {
    state = const AsyncValue.loading();
    try {
      state = AsyncValue.data(await _repo.getRiskRules());
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<void> toggle(String id, bool activo) async {
    await _repo.toggleRiskRule(id, activo);
    await load();
  }
}

// ── Fee Schedule ──────────────────────────────────────────────────────────────
final feeScheduleProvider =
    StateNotifierProvider<FeeScheduleNotifier, AsyncValue<List<FeeSchedule>>>((ref) {
  return FeeScheduleNotifier(ref.watch(adminRepositoryProvider));
});

class FeeScheduleNotifier extends StateNotifier<AsyncValue<List<FeeSchedule>>> {
  FeeScheduleNotifier(this._repo) : super(const AsyncValue.loading()) {
    load();
  }
  final AdminRepository _repo;

  Future<void> load() async {
    state = const AsyncValue.loading();
    try {
      state = AsyncValue.data(await _repo.getFees());
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<void> createFee(Map<String, dynamic> data) async {
    await _repo.createFee(data);
    await load();
  }

  Future<void> updateFee(String id, Map<String, dynamic> data) async {
    await _repo.updateFee(id, data);
    await load();
  }

  Future<void> deleteFee(String id) async {
    await _repo.deleteFee(id);
    await load();
  }
}

// ── Maintenance ───────────────────────────────────────────────────────────────
final maintenanceProvider =
    StateNotifierProvider<MaintenanceNotifier, AsyncValue<List<MaintenanceWindow>>>((ref) {
  return MaintenanceNotifier(ref.watch(adminRepositoryProvider));
});

class MaintenanceNotifier extends StateNotifier<AsyncValue<List<MaintenanceWindow>>> {
  MaintenanceNotifier(this._repo) : super(const AsyncValue.loading()) {
    load();
  }
  final AdminRepository _repo;

  Future<void> load() async {
    state = const AsyncValue.loading();
    try {
      state = AsyncValue.data(await _repo.getMaintenanceWindows());
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<void> toggle(String id, bool activate) async {
    await _repo.toggleMaintenance(id, activate);
    await load();
  }
}

// ── Audit Log ─────────────────────────────────────────────────────────────────
final auditLogProvider =
    StateNotifierProvider<AuditLogNotifier, AsyncValue<Map<String, dynamic>>>((ref) {
  return AuditLogNotifier(ref.watch(adminRepositoryProvider));
});

class AuditLogNotifier extends StateNotifier<AsyncValue<Map<String, dynamic>>> {
  AuditLogNotifier(this._repo) : super(const AsyncValue.loading()) {
    load();
  }
  final AdminRepository _repo;
  int _page = 0;

  Future<void> load({int page = 0}) async {
    _page = page;
    state = const AsyncValue.loading();
    try {
      state = AsyncValue.data(await _repo.getAuditLog(page: page));
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<void> nextPage() => load(page: _page + 1);
  Future<void> prevPage() => load(page: _page > 0 ? _page - 1 : 0);
}
