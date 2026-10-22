import 'dart:convert';

import 'package:crypto/crypto.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/core/config/app_config.dart';
import 'package:met/core/network/api_client_provider.dart';
import 'package:met/core/router/app_router.dart';
import 'package:met/core/session/session_expired_provider.dart';
import 'package:met/features/auth/presentation/providers/auth_provider.dart';

import '../../helpers/fake_dio.dart';

/// El cliente HTTP real de la app (interceptores de sesión, firma y renovación), con la red simulada.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late FakeDioAdapter server;
  late FakeDioAdapter refreshServer;
  late ProviderContainer container;
  late GoRouter router;
  late Dio api;

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({'access_token': 'acc-old', 'refresh_token': 'ref-1', 'user_id': 'user-1'});
    server = FakeDioAdapter();
    refreshServer = FakeDioAdapter();
    router = GoRouter(initialLocation: '/home', routes: [
      GoRoute(path: '/home', builder: (_, __) => const SizedBox()),
      GoRoute(path: '/login', builder: (_, __) => const SizedBox()),
    ]);
    container = ProviderContainer(overrides: [
      authRefreshClientProvider.overrideWithValue(fakeDio(refreshServer)),
      appRouterProvider.overrideWithValue(router),
    ]);
    api = container.read(apiClientProvider)..httpClientAdapter = server;
  });

  tearDown(() {
    container.dispose();
    router.dispose();
  });

  /// Ruta pedida al router (aquí no hay árbol de widgets que la construya).
  String location() => router.routeInformationProvider.value.uri.toString();

  group('Cabeceras', () {
    test('envía el token de la sesión del socio', () async {
      server.on('GET', '/v1/accounts/me', const FakeResponse({}));

      await api.get('/v1/accounts/me');

      expect(server.last.headers['Authorization'], 'Bearer acc-old');
      expect(server.last.headers.containsKey('X-Signature'), isFalse, reason: 'las lecturas no se firman');
    });

    test('usa el token de administrador en las rutas de administración', () async {
      await const FlutterSecureStorage().write(key: 'admin_access_token', value: 'admin-1');
      server.on('GET', '/v1/compliance/alerts', const FakeResponse([]));

      await api.get('/v1/compliance/alerts');

      expect(server.last.headers['Authorization'], 'Bearer admin-1');
    });

    test('firma con HMAC las operaciones que cambian datos', () async {
      server.on('POST', '/v1/accounts/transactions/transfer', const FakeResponse({}));
      final body = {'amount': 150000, 'concept': 'Arriendo'};

      await api.post('/v1/accounts/transactions/transfer', data: body);

      final headers = server.last.headers;
      final signed = 'POST/v1/accounts/transactions/transfer${headers['X-Timestamp']}${jsonEncode(body)}';
      final expected = base64Encode(Hmac(sha256, utf8.encode(AppConfig.hmacSecret)).convert(utf8.encode(signed)).bytes);
      expect(headers['X-Signature'], expected);
    });

    test('adjunta el captcha una sola vez al ingreso', () async {
      await const FlutterSecureStorage().write(key: 'temp_captcha_token', value: 'captcha-1');
      server.on('POST', '/v1/auth/login', const FakeResponse({}));

      await api.post('/v1/auth/login', data: {});
      expect(server.last.headers['X-Captcha-Token'], 'captcha-1');

      await api.post('/v1/auth/login', data: {});
      expect(server.last.headers.containsKey('X-Captcha-Token'), isFalse, reason: 'el captcha es de un solo uso');
    });
  });

  group('Sesión vencida', () {
    test('renueva el token con el refresh token y repite la petición', () async {
      server.on('GET', '/v1/accounts/me', const FakeResponse({'message': 'Token vencido'}, status: 401));
      refreshServer.on('POST', '/v1/auth/refresh', const FakeResponse({'accessToken': 'acc-new', 'refreshToken': 'ref-2'}));
      // El servidor acepta el token nuevo en el reintento.
      server.onRequest = (request) =>
          request.headers['Authorization'] == 'Bearer acc-new' ? const FakeResponse({'accountNumber': '4510009283'}) : null;

      final response = await api.get('/v1/accounts/me');

      expect(response.data, {'accountNumber': '4510009283'});
      expect(refreshServer.last.headers['Authorization'], 'Bearer ref-1');
      expect(await const FlutterSecureStorage().read(key: 'access_token'), 'acc-new');
      expect(await const FlutterSecureStorage().read(key: 'refresh_token'), 'ref-2');
      expect(container.read(sessionExpiredProvider), isFalse);
    });

    test('varias peticiones vencidas a la vez comparten una sola renovación', () async {
      server.on('GET', '/v1/accounts/me', const FakeResponse({}, status: 401));
      server.on('GET', '/v1/notifications', const FakeResponse({}, status: 401));
      refreshServer.on('POST', '/v1/auth/refresh', const FakeResponse({'accessToken': 'acc-new', 'refreshToken': 'ref-2'}));
      server.onRequest = (request) => request.headers['Authorization'] == 'Bearer acc-new' ? const FakeResponse({}) : null;

      await Future.wait([api.get('/v1/accounts/me'), api.get('/v1/notifications')]);

      expect(refreshServer.requests, hasLength(1));
    });

    test('si no puede renovar, cierra la sesión local y lleva al ingreso con aviso', () async {
      server.on('GET', '/v1/accounts/me', const FakeResponse({}, status: 401));
      refreshServer.on('POST', '/v1/auth/refresh', const FakeResponse({}, status: 401));

      await expectLater(api.get('/v1/accounts/me'), throwsA(isA<DioException>()));

      expect(await const FlutterSecureStorage().read(key: 'access_token'), isNull);
      expect(container.read(authStateProvider).valueOrNull, isNull);
      expect(container.read(sessionExpiredProvider), isTrue);
      expect(location(), '/login');
    });

    test('un PIN incorrecto en el ingreso no se trata como sesión vencida', () async {
      server.on('POST', '/v1/auth/login', const FakeResponse({'code': 'INVALID_CREDENTIALS'}, status: 401));

      await expectLater(api.post('/v1/auth/login', data: {}), throwsA(isA<DioException>()));

      expect(refreshServer.requests, isEmpty);
      expect(container.read(sessionExpiredProvider), isFalse);
      expect(location(), '/home');
    });
  });
}
