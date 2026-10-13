import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:crypto/crypto.dart';
import 'dart:convert';

import '../platform/platform.dart';
import '../config/app_config.dart';
import '../storage/secure_storage_service.dart';
import '../session/session_expired_provider.dart';
import '../router/app_router.dart';
import '../../features/auth/presentation/providers/auth_provider.dart';

final Provider<Dio> apiClientProvider = Provider<Dio>((ref) {
  final dio = Dio(BaseOptions(
    baseUrl: AppConfig.apiBaseUrl,
    connectTimeout: const Duration(seconds: 30),
    receiveTimeout: const Duration(seconds: 30),
  ));

  // Varias requests pueden recibir 401 casi al mismo tiempo (p. ej. al volver
  // de background con el access token ya vencido); esto evita disparar un
  // refresh por cada una y en su lugar comparten el mismo intento en curso.
  Future<String?>? refreshInFlight;

  Future<String?> refreshAccessToken() {
    return refreshInFlight ??= () async {
      try {
        final storage = ref.read(secureStorageProvider);
        final refreshToken = await storage.readRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty) return null;

        final refreshDio = Dio(BaseOptions(baseUrl: AppConfig.apiBaseUrl));
        final response = await refreshDio.post(
          '/v1/auth/refresh',
          options: Options(headers: {'Authorization': 'Bearer $refreshToken'}),
        );

        final newAccessToken = response.data['accessToken'] as String;
        final newRefreshToken = response.data['refreshToken'] as String;
        await storage.saveAccessToken(newAccessToken);
        await storage.saveRefreshToken(newRefreshToken);
        return newAccessToken;
      } catch (_) {
        return null;
      } finally {
        refreshInFlight = null;
      }
    }();
  }

  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) async {
      final storage = ref.read(secureStorageProvider);
      String? token;

      if (options.path.startsWith('/v1/admin/') ||
          options.path.startsWith('/v1/compliance/')) {
        token =
            await const FlutterSecureStorage().read(key: 'admin_access_token');
      } else {
        token = await storage.readAccessToken();
      }

      if (token != null && token.isNotEmpty) {
        options.headers['Authorization'] = 'Bearer $token';
      }

      // NOTA: dio 5.x construye la URL final con concatenación simple
      // (baseUrl + path, ver RequestOptions.uri en options.dart). NO usar
      // aquí ninguna normalización que quite el '/' inicial del path: como
      // AppConfig.apiBaseUrl no termina en '/', eso fusiona el segmento final
      // de baseUrl con el path (p.ej. ".../api" + "v1/..." = ".../apiv1/...").

      // HMAC Signature for state-modifying requests
      final method = options.method.toUpperCase();
      if (['POST', 'PUT', 'PATCH', 'DELETE'].contains(method)) {
        final timestamp = DateTime.now().millisecondsSinceEpoch.toString();

        final bodyString = options.data != null ? jsonEncode(options.data) : '';
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

      // 🛡️ Cloudflare Turnstile CAPTCHA para endpoints publicos críticos
      final protectedPaths = [
        '/v1/auth/login',
        '/v1/auth/register',
        '/v1/auth/pin-recovery/request'
      ];
      final pathToCheck = options.path.startsWith('http')
          ? Uri.parse(options.path).path
          : options.path;

      if (protectedPaths.any((p) => pathToCheck.endsWith(p))) {
        final captchaToken = await storage.read(key: 'temp_captcha_token');
        if (captchaToken != null && captchaToken.isNotEmpty) {
          options.headers['X-Captcha-Token'] = captchaToken;
          await storage.delete(key: 'temp_captcha_token');
        }
      }

      // 📱 Integridad del Dispositivo (Play Integrity / App Attest)
      // En un entorno real, este token se renueva periódicamente usando las APIs nativas
      // (ej. paquete `device_check` en iOS o `play_integrity` en Android).
      final attestationToken =
          await storage.read(key: 'device_attestation_token');
      final platformName = currentPlatformName();

      if (attestationToken != null && attestationToken.isNotEmpty) {
        options.headers['X-Device-Attestation'] = attestationToken;
        options.headers['X-Platform'] = platformName;
      } else if (kDebugMode) {
        // Mock para desarrollo local
        options.headers['X-Device-Attestation'] = 'dev-attestation-token-123';
        options.headers['X-Platform'] = platformName;
      }

      return handler.next(options);
    },
    onError: (error, handler) async {
      final path = error.requestOptions.path;
      // Un 401 en /v1/auth/login o /v1/auth/register es simplemente "credenciales
      // inválidas" del propio intento de login, no una sesión que expiró — no
      // debe forzar un logout/redirect global.
      final isAuthEntryPoint = path.contains('/v1/auth/login') ||
          path.contains('/v1/auth/register') ||
          path.contains('/v1/auth/refresh');

      final alreadyRetried = error.requestOptions.extra['retriedAfterRefresh'] == true;

      if (error.response?.statusCode == 401 && !isAuthEntryPoint && !alreadyRetried) {
        // El access token dura 30 min; en un flujo largo (p. ej. tomar fotos
        // + firmar en el registro biométrico) es normal que venza a mitad de
        // camino. En vez de botar al usuario a Login y perder lo que llevaba
        // hecho, se intenta renovar la sesión sola con el refresh token antes
        // de rendirse.
        final newAccessToken = await refreshAccessToken();
        if (newAccessToken != null) {
          final retryOptions = error.requestOptions;
          retryOptions.headers['Authorization'] = 'Bearer $newAccessToken';
          retryOptions.extra['retriedAfterRefresh'] = true;
          try {
            final response = await dio.fetch(retryOptions);
            return handler.resolve(response);
          } catch (_) {
            // Si el reintento también falla, sigue el flujo normal de abajo.
          }
        }

        await ref.read(authStateProvider.notifier).forceLocalLogout();
        ref.read(sessionExpiredProvider.notifier).state = true;
        ref.read(appRouterProvider).go('/login');
      }

      return handler.next(error);
    },
  ));

  // 🔒 SSL Pinning real en Dio (anti man-in-the-middle). En web es no-op: el
  // navegador no expone la validación de certificados a la app.
  configureDioSecurity(dio, AppConfig.sslFingerprint);

  return dio;
});
