import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client_provider.dart';
import '../models/compliance_models.dart';

final complianceRemoteDataSourceProvider =
    Provider<ComplianceRemoteDataSource>((ref) {
  return ComplianceRemoteDataSource(ref.watch(apiClientProvider));
});

class ComplianceRemoteDataSource {
  ComplianceRemoteDataSource(this._dio);
  final Dio _dio;

  Future<List<ReportTypeInfo>> listReportTypes() async {
    final response = await _dio.get('/v1/compliance/reports/types');
    return (response.data as List)
        .map((e) => ReportTypeInfo.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<RegulatoryReport>> listReports({int? year, int? month}) async {
    final response = await _dio.get('/v1/compliance/reports', queryParameters: {
      if (year != null) 'year': year,
      if (month != null) 'month': month,
    });
    return (response.data as List)
        .map((e) => RegulatoryReport.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<RegulatoryReport> generateReport(GenerateReportRequest request) async {
    final response = await _dio.post('/v1/compliance/reports/generate',
        data: request.toJson());
    return RegulatoryReport.fromJson(response.data as Map<String, dynamic>);
  }

  Future<List<int>> downloadReport(String reportId) async {
    final response = await _dio.get<List<int>>(
      '/v1/compliance/reports/$reportId/download',
      options: Options(responseType: ResponseType.bytes),
    );
    return response.data ?? [];
  }
}
