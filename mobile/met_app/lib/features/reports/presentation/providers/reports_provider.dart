import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../data/datasources/reports_remote_datasource.dart';

class ReportExportState {
  ReportExportState({this.isLoading = false, this.error});

  final bool isLoading;
  final String? error;

  ReportExportState copyWith({bool? isLoading, String? error}) {
    return ReportExportState(isLoading: isLoading ?? this.isLoading, error: error);
  }
}

class ReportExportNotifier extends StateNotifier<ReportExportState> {
  ReportExportNotifier(this.ref) : super(ReportExportState());

  final Ref ref;
  static final _dateFormat = DateFormat('yyyy-MM-dd');

  Future<Uint8List?> export({
    required DateTime from,
    required DateTime to,
    required String format,
  }) async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final dataSource = ref.read(reportsRemoteDataSourceProvider);
      final bytes = await dataSource.exportReport(
        from: _dateFormat.format(from),
        to: _dateFormat.format(to),
        format: format,
      );
      state = state.copyWith(isLoading: false);
      return bytes;
    } on DioException catch (e) {
      state = state.copyWith(isLoading: false, error: _extractMessage(e));
      return null;
    } catch (e) {
      state = state.copyWith(isLoading: false, error: 'Ocurrió un error inesperado');
      return null;
    }
  }

  /// El request pide bytes crudos (responseType.bytes) para poder recibir el
  /// PDF/Excel — eso significa que un error del backend también llega como
  /// bytes en vez de JSON ya parseado, así que hay que decodificarlo a mano.
  String _extractMessage(DioException e) {
    try {
      final data = e.response?.data;
      if (data is List<int>) {
        final decoded = jsonDecode(utf8.decode(data));
        if (decoded is Map && decoded['message'] != null) {
          return decoded['message'] as String;
        }
      } else if (data is Map && data['message'] != null) {
        return data['message'] as String;
      }
    } catch (_) {
      // Si no se pudo decodificar, se usa el mensaje genérico de abajo.
    }
    return 'No fue posible generar el reporte';
  }
}

final reportExportProvider = StateNotifierProvider.autoDispose<ReportExportNotifier, ReportExportState>((ref) {
  return ReportExportNotifier(ref);
});
