import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../config/app_config.dart';
import '../storage/secure_storage_service.dart';

import 'dart:io';
import 'package:dio/io.dart';

final apiClientProvider = Provider<Dio>((ref) {
  final dio = Dio(BaseOptions(
    baseUrl: AppConfig.apiBaseUrl,
    connectTimeout: const Duration(seconds: 30),
    receiveTimeout: const Duration(seconds: 30),
  ));

  // ARQUITECTURA DE SSL PINNING 🛡️
  dio.httpClientAdapter = IOHttpClientAdapter(
    createHttpClient: () {
      final client = HttpClient();
      client.badCertificateCallback = (X509Certificate cert, String host, int port) {
        // En DESARROLLO LOCAL con HTTPS auto-firmado, permitimos conexiones.
        // En PRODUCCIÓN, debes comparar el cert.sha1 o cert.der con el de tu servidor:
        // final prodSha256 = 'TU_HASH_SHA256_AQUI';
        // return cert.sha256.toString() == prodSha256;
        
        return true; // <- Cambiar a FALSE en producción si no coincide el hash
      };
      return client;
    },
  );


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
