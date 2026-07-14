import 'package:dio/dio.dart';

/// Traduce excepciones técnicas en mensajes amigables para el usuario.
///
/// Uso:
/// ```dart
/// } catch (e) {
///   final msg = ErrorMapper.toUserMessage(e);
///   ScaffoldMessenger.of(context).showSnackBar(
///     SnackBar(content: Text(msg), backgroundColor: Colors.red),
///   );
/// }
/// ```
class ErrorMapper {
  ErrorMapper._();

  static String toUserMessage(Object error) {
    if (error is DioException) {
      return _mapDioException(error);
    }
    // Errores de negocio que el backend devuelve como String
    final msg = error.toString();
    if (msg.isNotEmpty && !msg.startsWith('Exception:') && msg.length < 200) {
      return msg;
    }
    return 'Ocurrió un error inesperado. Intenta de nuevo.';
  }

  static String _mapDioException(DioException error) {
    switch (error.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return 'Tiempo de conexión agotado. Verifica tu internet e intenta de nuevo.';

      case DioExceptionType.connectionError:
        return 'Sin conexión al servidor. Verifica tu red e intenta de nuevo.';

      case DioExceptionType.badResponse:
        return _mapHttpStatus(error);

      case DioExceptionType.cancel:
        return 'La solicitud fue cancelada.';

      default:
        return 'Error de red. Intenta de nuevo.';
    }
  }

  static String _mapHttpStatus(DioException error) {
    final status = error.response?.statusCode;
    final data = error.response?.data;

    // Intentar extraer el mensaje del backend primero
    if (data is Map) {
      final backendMessage = data['message'] as String?;
      if (backendMessage != null && backendMessage.isNotEmpty) {
        return backendMessage;
      }
    }

    // Fallback por código HTTP
    switch (status) {
      case 400:
        return 'Datos inválidos. Verifica la información ingresada.';
      case 401:
        return 'Sesión expirada. Inicia sesión de nuevo.';
      case 403:
        return 'No tienes permiso para realizar esta acción.';
      case 404:
        return 'El recurso solicitado no fue encontrado.';
      case 429:
        return 'Demasiados intentos. Por favor espera un momento antes de intentar de nuevo.';
      case 500:
      case 502:
      case 503:
        return 'Error en el servidor. Intenta más tarde.';
      default:
        return 'Error inesperado (código $status). Intenta de nuevo.';
    }
  }
}
