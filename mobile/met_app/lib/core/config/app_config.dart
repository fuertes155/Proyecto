import 'package:flutter/foundation.dart';

class AppConfig {
  static const String appName = 'Met';

  static String get apiBaseUrl {
    const fromEnv = String.fromEnvironment('API_BASE_URL');
    if (fromEnv.isNotEmpty) return fromEnv;

    // 'localhost' NO sirve para Android (ni emulador ni dispositivo físico):
    // en el emulador se refiere al propio emulador, no al host. Usamos la IP
    // de la red Wi-Fi del host, que en la práctica también funciona desde el
    // emulador (su NAT por defecto sale a la red del host), además del
    // dispositivo físico real conectado a la misma red.
    //
    // Si cambias de red Wi-Fi, esta IP cambiará — actualízala (o corre con
    // --dart-define=API_BASE_URL=http://TU_IP:8080/api). Para el emulador
    // también puedes forzar --dart-define=API_BASE_URL=http://10.0.2.2:8080/api
    // si por lo que sea la IP de host no le resuelve.
    return 'http://192.168.1.34:8080/api';
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
