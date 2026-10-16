import 'package:flutter/foundation.dart';

class AppConfig {
  static const String appName = 'Met';
  
  static String get apiBaseUrl {
    const fromEnv = String.fromEnvironment('API_BASE_URL');
    if (fromEnv.isNotEmpty) return fromEnv;
    
    if (kIsWeb) {
      // Publicado (build release) el panel se sirve en el mismo dominio que el backend: nginx de
      // producción lo expone en /admin y la API en /api, igual que el contenedor admin-web local.
      // Con `flutter run` (debug) el panel corre en otro puerto y apunta al backend local.
      return kReleaseMode ? '${Uri.base.origin}/api' : 'http://localhost:8080/api';
    }
    
    if (defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:8080/api';
    }
    
    return 'http://localhost:8080/api';
  }

  static String get hmacSecret {
    const fromEnv = String.fromEnvironment('HMAC_SECRET');
    if (fromEnv.isNotEmpty) return fromEnv;

    // Fallback para desarrollo local.
    return 'D3vHmacS3cr3tKey123!@#';
  }
}
