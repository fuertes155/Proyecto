import 'package:dio/dio.dart';
import '../models/admin_models.dart';

class AdminAuditRepository {
  AdminAuditRepository(this._dio);
  final Dio _dio;

  Future<List<AuditLogEntry>> getAuditLogs({int page = 0, int size = 20}) async {
    try {
      final response = await _dio.get('/v1/admin/audit-log', queryParameters: {
        'page': page,
        'size': size,
      });

      if (response.statusCode == 200) {
        final data = response.data['data'] as List;
        return data.map((e) => AuditLogEntry.fromJson(e)).toList();
      } else {
        throw Exception('Error al obtener logs de auditoría: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Fallo la conexión: $e');
    }
  }
}
