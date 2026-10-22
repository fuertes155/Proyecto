import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met/features/auth/presentation/pages/splash_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

const _session = {'access_token': 'acc-1', 'refresh_token': 'ref-1', 'user_id': 'user-1'};

/// Arranque de la app: retoma la sesión guardada, pero exige el bloqueo del celular antes de
/// abrir la billetera.
void main() {
  late FakeDioAdapter server;
  late List<String> localAuthCalls;

  setUp(() {
    server = FakeDioAdapter()
      ..on('GET', '/v1/auth/me', const FakeResponse({
        'id': 'user-1', 'firstName': 'Ana', 'lastName': 'Pérez', 'status': 'ACTIVE', 'kycStatus': 'APPROVED',
      }));
  });

  /// El celular tiene (o no) bloqueo configurado y el socio lo supera (o no).
  void givenDevice({required bool hasLock, bool unlocks = true, bool fails = false}) {
    localAuthCalls = [];
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('plugins.flutter.io/local_auth'), (call) async {
      localAuthCalls.add(call.method);
      if (fails) throw PlatformException(code: 'NotAvailable');
      return switch (call.method) {
        'isDeviceSupported' => hasLock,
        'authenticate' => unlocks,
        _ => false,
      };
    });
  }

  Future<String> launch(WidgetTester tester) async {
    final router = await pumpScreen(tester, const SplashPage(), server: server, destinations: {'/login', '/home'});
    await tester.pumpAndSettle();
    return currentPath(router);
  }

  testWidgets('sin sesión guardada va al ingreso', (tester) async {
    mockNativePlugins();
    givenDevice(hasLock: true);

    expect(await launch(tester), '/login');
    expect(server.requests, isEmpty);
    expect(localAuthCalls, isEmpty);
  });

  testWidgets('con sesión, pide el bloqueo del celular y abre el inicio', (tester) async {
    mockNativePlugins(storage: _session);
    givenDevice(hasLock: true);

    expect(await launch(tester), '/home');
    expect(localAuthCalls, contains('authenticate'));
  });

  testWidgets('si el socio no supera el bloqueo del celular vuelve al ingreso', (tester) async {
    mockNativePlugins(storage: _session);
    givenDevice(hasLock: true, unlocks: false);

    expect(await launch(tester), '/login');
  });

  testWidgets('si el celular no tiene bloqueo configurado entra sin pedirlo', (tester) async {
    mockNativePlugins(storage: _session);
    givenDevice(hasLock: false);

    expect(await launch(tester), '/home');
    expect(localAuthCalls, isNot(contains('authenticate')));
  });

  testWidgets('si la verificación del celular falla no abre la billetera', (tester) async {
    mockNativePlugins(storage: _session);
    givenDevice(hasLock: true, fails: true);

    expect(await launch(tester), '/login');
  });

  testWidgets('si el servidor ya no reconoce la sesión va al ingreso', (tester) async {
    mockNativePlugins(storage: _session);
    givenDevice(hasLock: true);
    server.on('GET', '/v1/auth/me', const FakeResponse({'message': 'Token inválido'}, status: 401));

    expect(await launch(tester), '/login');
  });
}
