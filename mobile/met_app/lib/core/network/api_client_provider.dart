import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../config/app_config.dart';
import '../storage/secure_storage_service.dart';

final apiClientProvider = Provider<Dio>((ref) {
  final dio = Dio(BaseOptions(
    baseUrl: AppConfig.apiBaseUrl,
    connectTimeout: const Duration(seconds: 30),
    receiveTimeout: const Duration(seconds: 30),
  ));

  // ARQUITECTURA DE SSL PINNING 🛡️
  // Desactivado temporalmente para permitir ejecución en Flutter Web (Chrome/Edge)
  /*
  dio.httpClientAdapter = IOHttpClientAdapter(
    createHttpClient: () {
      final client = HttpClient();
      client.badCertificateCallback = (X509Certificate cert, String host, int port) {
        return true; 
      };
      return client;
    },
  );
  */
  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) async {
      final storage = ref.read(secureStorageProvider);
      String? token;

      if (options.path.startsWith('/v1/admin/') || options.path.startsWith('/v1/compliance/')) {
        token = await const FlutterSecureStorage().read(key: 'admin_access_token');
      } else {
        token = await storage.readAccessToken();
      }

      if (token != null && token.isNotEmpty) {
        options.headers['Authorization'] = 'Bearer $token';
      }
      return handler.next(options);
    },
  ));

  return dio;
});
