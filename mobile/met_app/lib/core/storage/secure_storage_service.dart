import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:uuid/uuid.dart';

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
  static const _sessionTimeoutMinutes = 5;

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

  Future<String?> readUserId() => _storage.read(key: _userIdKey);

  Future<void> clear() => _storage.deleteAll();

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
