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
}
