import 'package:flutter/foundation.dart';

class AppConfig {
  static const String appName = 'Met';
  
  static String get apiBaseUrl {
    const fromEnv = String.fromEnvironment('API_BASE_URL');
    if (fromEnv.isNotEmpty) return fromEnv;
    
    if (kIsWeb) {
      return 'http://localhost:8080/api';
    }
    
    if (defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:8080/api';
    }
    
    return 'http://localhost:8080/api';
  }
}
