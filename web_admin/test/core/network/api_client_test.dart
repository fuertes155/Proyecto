import 'dart:convert';

import 'package:crypto/crypto.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met_admin/core/config/app_config.dart';
import 'package:met_admin/core/network/api_client_provider.dart';

import '../../helpers/fake_dio.dart';

/// Cliente HTTP del panel: qué token viaja según la ruta y la firma de las escrituras.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late FakeDioAdapter server;
  late ProviderContainer container;

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({'admin_access_token': 'admin-1', 'access_token': 'socio-1'});
    server = FakeDioAdapter()
      ..on('GET', '/v1/admin/limits', const FakeResponse([]))
      ..on('GET', '/v1/compliance/reports', const FakeResponse([]))
      ..on('GET', '/v1/accounts/me', const FakeResponse({}))
      ..on('POST', '/v1/compliance/reports/generate', const FakeResponse({}));
    container = ProviderContainer();
    container.read(apiClientProvider).httpClientAdapter = server;
  });

  tearDown(() => container.dispose());

  test('las rutas de administración y cumplimiento usan el token del administrador', () async {
    final api = container.read(apiClientProvider);

    await api.get('/v1/admin/limits');
    expect(server.last.headers['Authorization'], 'Bearer admin-1');

    await api.get('/v1/compliance/reports');
    expect(server.last.headers['Authorization'], 'Bearer admin-1');

    await api.get('/v1/accounts/me');
    expect(server.last.headers['Authorization'], 'Bearer socio-1');
  });

  test('firma con HMAC las escrituras y no las lecturas', () async {
    final api = container.read(apiClientProvider);
    final body = {'reportType': 'SARLAFT', 'periodYear': 2026, 'periodMonth': 9};

    await api.get('/v1/compliance/reports');
    expect(server.last.headers.containsKey('X-Signature'), isFalse);

    await api.post('/v1/compliance/reports/generate', data: body);
    final headers = server.last.headers;
    final signed = 'POST/v1/compliance/reports/generate${headers['X-Timestamp']}${jsonEncode(body)}';
    final expected = base64Encode(Hmac(sha256, utf8.encode(AppConfig.hmacSecret)).convert(utf8.encode(signed)).bytes);
    expect(headers['X-Signature'], expected);
  });

  test('sin token guardado no envía la cabecera de autorización', () async {
    FlutterSecureStorage.setMockInitialValues({});

    await container.read(apiClientProvider).get('/v1/admin/limits');

    expect(server.last.headers.containsKey('Authorization'), isFalse);
  });
}
