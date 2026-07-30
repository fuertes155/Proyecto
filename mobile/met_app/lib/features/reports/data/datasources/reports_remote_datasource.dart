import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client_provider.dart';

final reportsRemoteDataSourceProvider = Provider<ReportsRemoteDataSource>((ref) {
  return ReportsRemoteDataSource(ref.watch(apiClientProvider));
});

class ReportsRemoteDataSource {
  ReportsRemoteDataSource(this._dio);

  final Dio _dio;

  /// [from]/[to] en formato yyyy-MM-dd. [format]: 'pdf' o 'xlsx'.
  Future<Uint8List> exportReport({
    required String from,
    required String to,
    required String format,
  }) async {
    final response = await _dio.get<List<int>>(
      '/v1/reports/export',
      queryParameters: {'from': from, 'to': to, 'format': format},
      options: Options(responseType: ResponseType.bytes),
    );
    return Uint8List.fromList(response.data ?? const []);
  }
}
