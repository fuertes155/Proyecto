import 'dart:convert';

import 'package:crypto/crypto.dart';
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

      // Firma HMAC para peticiones que modifican estado — el backend
      // (HmacSignatureFilter) la exige en todo POST/PUT/PATCH/DELETE
      // salvo login y webhooks.
      final method = options.method.toUpperCase();
      if (['POST', 'PUT', 'PATCH', 'DELETE'].contains(method)) {
        final timestamp = DateTime.now().millisecondsSinceEpoch.toString();
        final bodyString = options.data != null ? jsonEncode(options.data) : '';
        final path = options.path.startsWith('http') ? Uri.parse(options.path).path : options.path;

        final dataToSign = method + path + timestamp + bodyString;
        final key = utf8.encode(AppConfig.hmacSecret);
        final bytes = utf8.encode(dataToSign);
        final signature = base64Encode(Hmac(sha256, key).convert(bytes).bytes);

        options.headers['X-Timestamp'] = timestamp;
        options.headers['X-Signature'] = signature;
      }

      return handler.next(options);
    },
  ));

  return dio;
});
