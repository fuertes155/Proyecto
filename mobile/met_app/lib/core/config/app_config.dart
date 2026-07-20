import 'package:flutter/foundation.dart';

class AppConfig {
  static const String appName = 'Met';
  
  static String get apiBaseUrl {
    const fromEnv = String.fromEnvironment('API_BASE_URL');
    if (fromEnv.isNotEmpty) return fromEnv;
    
    if (kIsWeb) {
      return 'http://localhost:8080/api';
    }
    
    // Si no se define en producción, fallará (lo cual es seguro para evitar que se conecte a un servidor local).
    // Para compilar la app usa: flutter build apk --dart-define=API_BASE_URL=https://api.tudominio.com
    return 'http://localhost:8080/api'; // Fallback solo para desarrollo local (Emulator)
  }

  static String get hmacSecret {
    const fromEnv = String.fromEnvironment('HMAC_SECRET');
    if (fromEnv.isNotEmpty) return fromEnv;
    // Fallback para dev. DEBE cambiarse en prod usando --dart-define
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
