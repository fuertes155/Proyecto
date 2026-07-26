import 'package:dio/dio.dart';

import '../../domain/repositories/home_repository.dart';
import '../models/home_summary_model.dart';

/// Implementación del repositorio de Home que obtiene datos de la API remota.
class HomeRemoteRepository implements HomeRepository {
  final Dio _dio;

  HomeRemoteRepository(this._dio);

  @override
  Future<HomeSummaryModel> getHomeSummary() async {
    try {
      final response = await _dio.get('/v1/account/summary');
      return HomeSummaryModel.fromJson(response.data['data']);
    } on DioException catch (e) {
      // Manejar error 404 (cuenta no encontrada) o similar y lanzar
      // una excepción de dominio.
      if (e.response?.statusCode == 404) {
         throw Exception('Usuario no tiene una cuenta bancaria activa.');
      }
      throw Exception('Error al obtener el resumen de la cuenta: ${e.message}');
    } catch (e) {
      throw Exception('Error inesperado: $e');
    }
  }
}
