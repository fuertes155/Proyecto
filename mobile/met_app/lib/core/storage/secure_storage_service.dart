import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:uuid/uuid.dart';

import '../config/app_config.dart';

final secureStorageProvider = Provider<SecureStorageService>((ref) {
  return SecureStorageService();
});

class SecureStorageService {
  static const _accessTokenKey = 'access_token';
  static const _refreshTokenKey = 'refresh_token';
  static const _userIdKey = 'user_id';
  static const _deviceIdKey = 'device_id';
  static const _lastActivityKey = 'last_activity_at';

  /// Tiempo máximo de inactividad antes de cerrar sesión (5 minutos)
  static const _sessionTimeoutMinutes = 15;

  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
    required String userId,
  }) async {
    await _storage.write(key: _accessTokenKey, value: accessToken);
    await _storage.write(key: _refreshTokenKey, value: refreshToken);
    await _storage.write(key: _userIdKey, value: userId);
  }

  Future<String> getDeviceId() async {
    var deviceId = await _storage.read(key: _deviceIdKey);
    if (deviceId == null) {
      deviceId = const Uuid().v4();
      await _storage.write(key: _deviceIdKey, value: deviceId);
    }
    return deviceId;
  }

  Future<String?> readAccessToken() => _storage.read(key: _accessTokenKey);

  Future<String?> readRefreshToken() => _storage.read(key: _refreshTokenKey);

  Future<void> saveAccessToken(String accessToken) =>
      _storage.write(key: _accessTokenKey, value: accessToken);

  Future<void> saveRefreshToken(String refreshToken) =>
      _storage.write(key: _refreshTokenKey, value: refreshToken);

  Future<String?> readUserId() => _storage.read(key: _userIdKey);

  // Claves que pertenecen al dispositivo y no a la sesión: sobreviven al cierre
  // de sesión.
  //  - device_id: si se borrara, cada inicio de sesión llegaría con un id recién
  //    generado y el backend pediría el código de "dispositivo nuevo" en todos
  //    los ingresos, no solo cuando el usuario cambia de teléfono.
  //  - URL del servidor configurada en la pantalla de desarrollador: si se
  //    borrara, la app volvería a la IP por defecto después de cada logout.
  static const _deviceScopedKeys = [_deviceIdKey, AppConfig.devApiBaseUrlKey];

  /// Borra la sesión (tokens y cualquier otro dato guardado) conservando solo
  /// las claves de [_deviceScopedKeys].
  Future<void> clear() async {
    final kept = <String, String>{};
    for (final key in _deviceScopedKeys) {
      final value = await _storage.read(key: key);
      if (value != null) kept[key] = value;
    }
    await _storage.deleteAll();
    for (final entry in kept.entries) {
      await _storage.write(key: entry.key, value: entry.value);
    }
  }
  
  // Proxy methods para acceso genérico
  Future<String?> read({required String key}) => _storage.read(key: key);
  Future<void> write({required String key, required String value}) => _storage.write(key: key, value: value);
  Future<void> delete({required String key}) => _storage.delete(key: key);

  /// Guarda el timestamp de la última actividad del usuario
  Future<void> saveLastActivity() async {
    await _storage.write(
      key: _lastActivityKey,
      value: DateTime.now().millisecondsSinceEpoch.toString(),
    );
  }

  /// Verifica si la sesión expiró por inactividad.
  /// Devuelve true si hay que cerrar sesión.
  Future<bool> isSessionExpired() async {
    final raw = await _storage.read(key: _lastActivityKey);
    if (raw == null) return false; // sin registro = sesión nueva, no expirada
    final lastActivity = DateTime.fromMillisecondsSinceEpoch(int.parse(raw));
    final elapsed = DateTime.now().difference(lastActivity);
    return elapsed.inMinutes >= _sessionTimeoutMinutes;
  }
}
