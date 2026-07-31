import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../../../core/network/api_client_provider.dart';
import '../models/admin_models.dart';

final adminRepositoryProvider = Provider<AdminRepository>((ref) {
  return AdminRepository(ref.watch(apiClientProvider));
});

class AdminRepository {
  AdminRepository(this._dio);

  final Dio _dio;
  static const _storage = FlutterSecureStorage();
  static const _adminTokenKey = 'admin_access_token';

  // ── Auth ──────────────────────────────────────────────────────────────────
  Future<AdminAuthResponse> login(AdminLoginRequest request) async {
    final response = await _dio.post('/v1/admin/auth/login', data: request.toJson());
    final auth = AdminAuthResponse.fromJson(response.data as Map<String, dynamic>);
    await _storage.write(key: _adminTokenKey, value: auth.accessToken);
    return auth;
  }

  Future<void> logout() async {
    await _storage.delete(key: _adminTokenKey);
  }

  Future<String?> getAdminToken() => _storage.read(key: _adminTokenKey);

  // ── Emergency Lock ─────────────────────────────────────────────────────────
  Future<void> emergencyLock(EmergencyLockRequest request) async {
    await _adminDio().post('/v1/admin/emergency-lock', data: request.toJson());
  }

  // ── Operation Limits ───────────────────────────────────────────────────────
  Future<List<OperationLimit>> getOperationLimits() async {
    final response = await _adminDio().get('/v1/admin/limits');
    return (response.data as List)
        .map((e) => OperationLimit.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<OperationLimit> updateLimit(String tipoOperacion, int montoDiario, int montoPorTx) async {
    final response = await _adminDio().put('/v1/admin/limits', data: {
      'tipoOperacion': tipoOperacion,
      'montoDiarioMax': montoDiario,
      'montoPorTransaccionMax': montoPorTx,
    });
    return OperationLimit.fromJson(response.data as Map<String, dynamic>);
  }

  // ── Risk Rules ─────────────────────────────────────────────────────────────
  Future<List<RiskRule>> getRiskRules() async {
    final response = await _adminDio().get('/v1/admin/risk-rules');
    return (response.data as List)
        .map((e) => RiskRule.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> toggleRiskRule(String id, bool activo) async {
    await _adminDio().patch('/v1/admin/risk-rules/$id/toggle?activo=$activo');
  }

  Future<RiskRule> createRiskRule(Map<String, dynamic> data) async {
    final response = await _adminDio().post('/v1/admin/risk-rules', data: data);
    return RiskRule.fromJson(response.data as Map<String, dynamic>);
  }

  // ── Fee Schedule ───────────────────────────────────────────────────────────
  Future<List<FeeSchedule>> getFees() async {
    final response = await _adminDio().get('/v1/admin/fees');
    return (response.data as List)
        .map((e) => FeeSchedule.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<FeeSchedule> createFee(Map<String, dynamic> data) async {
    final response = await _adminDio().post('/v1/admin/fees', data: data);
    return FeeSchedule.fromJson(response.data as Map<String, dynamic>);
  }

  Future<FeeSchedule> updateFee(String id, Map<String, dynamic> data) async {
    final response = await _adminDio().put('/v1/admin/fees/$id', data: data);
    return FeeSchedule.fromJson(response.data as Map<String, dynamic>);
  }

  Future<void> deleteFee(String id) async {
    await _adminDio().delete('/v1/admin/fees/$id');
  }


  // ── Maintenance ────────────────────────────────────────────────────────────
  Future<List<MaintenanceWindow>> getMaintenanceWindows() async {
    final response = await _adminDio().get('/v1/admin/maintenance');
    return (response.data as List)
        .map((e) => MaintenanceWindow.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> toggleMaintenance(String id, bool activate) async {
    final action = activate ? 'activate' : 'deactivate';
    await _adminDio().patch('/v1/admin/maintenance/$id/$action');
  }

  // ── Audit Log ──────────────────────────────────────────────────────────────
  Future<Map<String, dynamic>> getAuditLog({int page = 0, int size = 20}) async {
    final response = await _adminDio().get('/v1/admin/audit-log?page=$page&size=$size');
    return response.data as Map<String, dynamic>;
  }

  // ── SARLAFT / Cumplimiento ─────────────────────────────────────────────────
  Future<Map<String, dynamic>> getComplianceAlerts({
    String status = 'OPEN',
    int page = 0,
    int size = 20,
  }) async {
    final response = await _adminDio()
        .get('/v1/admin/compliance/alerts?status=$status&page=$page&size=$size');
    return response.data as Map<String, dynamic>;
  }

  Future<ComplianceAlert> reviewComplianceAlert(String id, String status, String? notes) async {
    final response = await _adminDio().post('/v1/admin/compliance/alerts/$id/review', data: {
      'status': status,
      if (notes != null && notes.isNotEmpty) 'notes': notes,
    });
    return ComplianceAlert.fromJson(response.data as Map<String, dynamic>);
  }

  Future<List<RestrictiveListMatch>> getRestrictiveListMatches({int page = 0, int size = 20}) async {
    final response = await _adminDio()
        .get('/v1/admin/compliance/restrictive-list-matches?page=$page&size=$size');
    return (response.data as List)
        .map((e) => RestrictiveListMatch.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<Map<String, dynamic>> refreshRestrictiveLists() async {
    final response = await _adminDio().post('/v1/admin/compliance/restrictive-lists/refresh');
    return response.data as Map<String, dynamic>;
  }

  // ── Reset Credentials ──────────────────────────────────────────────────────
  Future<void> resetUserCredentials(String userId, String reason) async {
    await _adminDio().post('/v1/admin/users/$userId/reset-credentials', data: {'reason': reason});
  }

  // ── Transaction Reversal ───────────────────────────────────────────────────
  Future<void> reverseTransaction(String txId, String reason, String confirmCode) async {
    await _adminDio().post('/v1/admin/transactions/reverse', data: {
      'transactionId': txId,
      'reason': reason,
      'confirmationCode': confirmCode,
    });
  }

  // ── Helper ─────────────────────────────────────────────────────────────────
  Dio _adminDio() {
    // Retorna el mismo Dio — el interceptor en api_client_provider
    // agrega el token automáticamente desde secure storage.
    return _dio;
  }
}
