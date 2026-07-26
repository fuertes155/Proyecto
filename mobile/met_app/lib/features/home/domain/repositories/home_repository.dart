import '../entities/home_summary.dart';

/// Contrato (interfaz) del repositorio del home.
/// La capa de datos implementa esta interfaz; el dominio solo conoce la interfaz.
abstract class HomeRepository {
  /// Obtiene el resumen financiero del usuario autenticado.
  Future<HomeSummary> getHomeSummary();
}
