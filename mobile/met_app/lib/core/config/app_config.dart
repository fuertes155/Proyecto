import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class AppConfig {
  static const String appName = 'Met';

  // Clave donde se guarda la URL del servidor configurada en runtime desde la
  // pantalla de desarrollador del login. Sobrescribe la IP por defecto sin
  // recompilar el APK — útil con Tailscale (IP fija por dispositivo) o al
  // cambiar de red Wi-Fi.
  static const String _devApiBaseUrlKey = 'dev_api_base_url';

  static const FlutterSecureStorage _storage = FlutterSecureStorage();

  // Cargado una sola vez en main() antes de runApp(); null = sin override.
  static String? _runtimeApiBaseUrl;

  /// Lee el override de URL guardado en disco. Llamar en main() ANTES de runApp
  /// para que el cliente HTTP (Dio) se construya ya con la URL correcta.
  static Future<void> loadRuntimeOverrides() async {
    try {
      final saved = await _storage.read(key: _devApiBaseUrlKey);
      if (saved != null && saved.trim().isNotEmpty) {
        _runtimeApiBaseUrl = saved.trim();
      }
    } catch (_) {
      // Si el secure storage falla (p. ej. en tests), seguimos con el default.
    }
  }

  /// Guarda (o borra, si [url] es null/vacío) el override de URL del servidor.
  /// Requiere reiniciar la app para que Dio tome el cambio.
  static Future<void> setDevApiBaseUrl(String? url) async {
    final value = url?.trim() ?? '';
    if (value.isEmpty) {
      await _storage.delete(key: _devApiBaseUrlKey);
      _runtimeApiBaseUrl = null;
    } else {
      await _storage.write(key: _devApiBaseUrlKey, value: value);
      _runtimeApiBaseUrl = value;
    }
  }

  /// Override activo (para mostrarlo en la UI de desarrollador). null si no hay.
  static String? get devApiBaseUrlOverride => _runtimeApiBaseUrl;

  static String get apiBaseUrl {
    // 1. --dart-define en tiempo de compilación (máxima prioridad, para CI/release).
    const fromEnv = String.fromEnvironment('API_BASE_URL');
    if (fromEnv.isNotEmpty) return fromEnv;

    // 2. Override configurado en runtime desde la pantalla de desarrollador.
    final runtime = _runtimeApiBaseUrl;
    if (runtime != null && runtime.isNotEmpty) return runtime;

    // 3. Defaults por plataforma.
    //
    // 'localhost' NO sirve para Android (ni emulador ni dispositivo físico):
    // en el emulador se refiere al propio emulador, no al host. Para un
    // dispositivo físico lo más robusto es Tailscale: instala Tailscale en la
    // PC y en el teléfono, y pon aquí (o mejor, en la pantalla de desarrollador
    // del login) la IP 100.x.y.z de la PC — no cambia aunque cambies de red.
    //
    // Alternativas: --dart-define=API_BASE_URL=http://TU_IP:8080/api, o
    // http://10.0.2.2:8080/api para el emulador.
    if (kIsWeb) {
      return 'http://localhost:8080/api';
    }

    return 'http://192.168.0.102:8080/api';
  }

  static String get hmacSecret {
    const fromEnv = String.fromEnvironment('HMAC_SECRET');
    if (fromEnv.isNotEmpty) return fromEnv;

    // En release NO hay fallback: un build de producción sin --dart-define=HMAC_SECRET=...
    // firmaría todo el tráfico con un secreto público, inutilizando el control. Mejor que
    // el build falle en arranque a que salga así a producción en silencio.
    if (kReleaseMode) {
      throw StateError(
        'HMAC_SECRET no fue provisto en el build de release. '
        'Compila con --dart-define=HMAC_SECRET=<secreto real>.',
      );
    }

    // Fallback solo para desarrollo local (debug/profile).
    return 'D3vHmacS3cr3tKey123!@#';
  }

  static String get sslFingerprint {
    const fromEnv = String.fromEnvironment('SSL_FINGERPRINT');
    if (fromEnv.isNotEmpty) return fromEnv;
    // Placeholder para desarrollo.
    // Compilar con: --dart-define=SSL_FINGERPRINT=A1:B2...
    return 'A1:B2:C3:D4:E5:F6:78:90:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF';
  }
}
