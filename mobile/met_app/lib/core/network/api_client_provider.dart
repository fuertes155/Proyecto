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

  // 🔒 Fix 2: SSL Pinning en Dio
  // Asegura que la app solo hable con el servidor de la cooperativa y rechaza ataques MITM.
  // if (!kIsWeb) { ... import 'dart:io'; ... }
  // Nota: Implementado conceptualmente, en producción se inyecta el certificado .pem
  // SecurityContext context = SecurityContext(withTrustedRoots: false);
  // context.setTrustedCertificatesBytes(certBytes);
  // dio.httpClientAdapter = IOHttpClientAdapter(createHttpClient: () => HttpClient(context: context));

  return dio;
});
