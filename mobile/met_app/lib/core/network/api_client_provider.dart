import 'dart:io';
import 'package:dio/dio.dart';
import 'package:dio/io.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:crypto/crypto.dart';
import 'dart:convert';

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

      // HMAC Signature for state-modifying requests
      final method = options.method.toUpperCase();
      if (['POST', 'PUT', 'PATCH', 'DELETE'].contains(method)) {
        final timestamp = DateTime.now().millisecondsSinceEpoch.toString();
        
        // El body puede ser un Map o String (Json). Dio por defecto usa Map.
        final bodyString = options.data != null ? jsonEncode(options.data) : '';
        // Path relativo, Ej: /v1/auth/login. En Dio, options.path ya suele ser relativo.
        final path = options.path.startsWith('http') 
            ? Uri.parse(options.path).path 
            : options.path;

        final dataToSign = method + path + timestamp + bodyString;
        
        final key = utf8.encode(AppConfig.hmacSecret);
        final bytes = utf8.encode(dataToSign);
        
        final hmacSha256 = Hmac(sha256, key);
        final digest = hmacSha256.convert(bytes);
        final signature = base64Encode(digest.bytes);

        options.headers['X-Timestamp'] = timestamp;
        options.headers['X-Signature'] = signature;
      }

      return handler.next(options);
    },
  ));

  // 🔒 Fix 2: SSL Pinning Real en Dio (Anti Man-in-the-Middle)
  if (!kIsWeb) {
    dio.httpClientAdapter = IOHttpClientAdapter(
      createHttpClient: () {
        // Al instanciar SecurityContext con withTrustedRoots: false, 
        // Dart desconfía de TODOS los certificados por defecto (incluso los del OS).
        // Esto fuerza a que TODAS las conexiones pasen por badCertificateCallback.
        final SecurityContext context = SecurityContext(withTrustedRoots: false);
        final client = HttpClient(context: context);
        
        // Huella dactilar (SHA-256) del certificado público de nuestro servidor en Producción.
        // TODO: Reemplazar con el hash real del certificado de tu dominio.
        const String expectedFingerprint = 'A1:B2:C3:D4:E5:F6:78:90:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF';

        client.badCertificateCallback = (X509Certificate cert, String host, int port) {
          // Excepción para desarrollo local
          if (host.contains('localhost') || host.contains('10.0.2.2')) {
            return true;
          }
          
          // Anclaje estricto: Comparamos el hash SHA-256 del certificado recibido
          // contra nuestra huella esperada (sin importar lo que diga el SO).
          // NOTA: cert.sha1 o cert.der se usan comúnmente, aquí simulamos una validación
          // usando el formato estándar o parseándolo.
          
          // Dart X509Certificate provee sha1 por defecto. Para mayor seguridad,
          // se extrae el byte array (der) y se calcula el sha256.
          final digest = sha256.convert(cert.der);
          final actualFingerprint = digest.bytes
              .map((b) => b.toRadixString(16).padLeft(2, '0').toUpperCase())
              .join(':');

          if (actualFingerprint == expectedFingerprint) {
            return true; // Es nuestro servidor legítimo
          }

          // Si llega aquí, es un ataque MITM o el certificado cambió sin actualizar la app.
          debugPrint('🚨 [CRITICAL] SSL Pinning falló. Ataque MITM detectado para host $host');
          return false; // Rechaza la conexión inmediatamente
        };
        return client;
      },
    );
  }

  return dio;
});
