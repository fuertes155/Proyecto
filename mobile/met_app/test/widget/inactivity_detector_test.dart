import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/core/network/api_client_provider.dart';
import 'package:met/core/router/app_router.dart';
import 'package:met/core/session/inactivity_detector.dart';
import 'package:met/features/auth/presentation/providers/auth_provider.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

/// Cierre de sesión por inactividad, montado como en main.dart: el detector envuelve el enrutador
/// desde el builder de MaterialApp.router.
void main() {
  const aviso = 'Tu sesión expiró por inactividad. Ingresa tu PIN nuevamente.';
  const huella = MethodChannel('plugins.flutter.io/local_auth');
  late FakeDioAdapter server;
  late GoRouter router;

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter()..on('POST', '/v1/auth/logout', const FakeResponse({}));
  });

  Future<ProviderContainer> abrirApp(WidgetTester tester,
      {bool conSesion = true, DateTime Function() now = DateTime.now}) async {
    router = GoRouter(initialLocation: '/home', routes: [
      GoRoute(path: '/home', builder: (_, __) => const Scaffold(body: Center(child: Text('Saldo del socio')))),
      GoRoute(path: '/login', builder: (_, __) => const Scaffold(body: Text('Pantalla de ingreso'))),
    ]);
    addTearDown(router.dispose);

    await tester.pumpWidget(ProviderScope(
      overrides: [
        apiClientProvider.overrideWithValue(fakeDio(server)),
        appRouterProvider.overrideWithValue(router),
        if (conSesion)
          signedIn(const {
            'id': 'user-1', 'firstName': 'Ana', 'lastName': 'Pérez', 'email': 'socio@example.com', 'status': 'ACTIVE',
          }),
      ],
      child: MaterialApp.router(
        routerConfig: router,
        builder: (context, child) => InactivityDetector(now: now, child: child ?? const SizedBox.shrink()),
      ),
    ));
    await tester.pump();
    return ProviderScope.containerOf(tester.element(find.byType(InactivityDetector)));
  }

  /// Deja terminar el cierre de sesión (petición al servidor falso) y la navegación.
  Future<void> esperar(WidgetTester tester) async {
    for (var i = 0; i < 6; i++) {
      await tester.pump(const Duration(milliseconds: 100));
    }
  }

  void sesionCerradaEnElIngreso(ProviderContainer app) {
    expect(app.read(authStateProvider).valueOrNull, isNull);
    expect(server.requests.where((r) => r.path.endsWith('/v1/auth/logout')), hasLength(1));
    expect(find.text(aviso), findsOneWidget);
    expect(currentPath(router), '/login');
    expect(find.text('Pantalla de ingreso'), findsOneWidget);
  }

  testWidgets('tras 15 minutos sin tocar la pantalla cierra la sesión y lleva al ingreso', (tester) async {
    final app = await abrirApp(tester);

    await tester.pump(const Duration(minutes: 15));
    await esperar(tester);

    sesionCerradaEnElIngreso(app);
  });

  testWidgets('cada toque reinicia el conteo de los 15 minutos', (tester) async {
    final app = await abrirApp(tester);

    await tester.pump(const Duration(minutes: 14));
    await tester.tap(find.text('Saldo del socio'));
    await tester.pump(const Duration(minutes: 14));
    expect(app.read(authStateProvider).valueOrNull, isNotNull);
    expect(currentPath(router), '/home');

    await tester.pump(const Duration(minutes: 1));
    await esperar(tester);
    sesionCerradaEnElIngreso(app);
  });

  testWidgets('sin sesión iniciada no hace nada', (tester) async {
    await abrirApp(tester, conSesion: false);

    await tester.pump(const Duration(minutes: 20));
    await esperar(tester);

    expect(find.text(aviso), findsNothing);
    expect(currentPath(router), '/home');
    expect(server.requests, isEmpty);
  });

  testWidgets('al volver a la app después de 15 minutos fuera cierra la sesión', (tester) async {
    final app = await abrirApp(tester);

    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    await tester.pump();
    // Lo que queda guardado al salir: la última actividad fue hace 20 minutos.
    final hace20Minutos = DateTime.now().subtract(const Duration(minutes: 20)).millisecondsSinceEpoch;
    FlutterSecureStorage.setMockInitialValues({'last_activity_at': '$hace20Minutos'});
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await esperar(tester);

    sesionCerradaEnElIngreso(app);
  });

  group('al volver del segundo plano', () {
    late List<String> llamadas;
    late Object? Function(MethodCall) respuesta;

    setUp(() {
      llamadas = [];
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(huella, (call) async {
        llamadas.add(call.method);
        return respuesta(call);
      });
    });

    Object? celularConHuella(MethodCall call, {required Object? alAutenticar}) => switch (call.method) {
          'getAvailableBiometrics' => <String>['fingerprint'],
          'authenticate' => alAutenticar,
          _ => true,
        };

    testWidgets('tras más de 10 segundos pide la huella y, si el socio cancela, cierra la sesión', (tester) async {
      respuesta = (call) => celularConHuella(call, alAutenticar: false);
      var ahora = DateTime(2026, 10, 30, 9);
      final app = await abrirApp(tester, now: () => ahora);

      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      ahora = ahora.add(const Duration(seconds: 11));
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await esperar(tester);

      expect(llamadas, contains('authenticate'));
      sesionCerradaEnElIngreso(app);
    });

    testWidgets('si la huella confirma al socio, la sesión sigue', (tester) async {
      respuesta = (call) => celularConHuella(call, alAutenticar: true);
      var ahora = DateTime(2026, 10, 30, 9);
      final app = await abrirApp(tester, now: () => ahora);

      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      ahora = ahora.add(const Duration(minutes: 2));
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await esperar(tester);

      expect(llamadas, contains('authenticate'));
      expect(app.read(authStateProvider).valueOrNull, isNotNull);
      expect(currentPath(router), '/home');
    });

    testWidgets('si la huella falla con un error, por seguridad cierra la sesión', (tester) async {
      respuesta = (call) => call.method == 'authenticate'
          ? throw PlatformException(code: 'NotAvailable')
          : celularConHuella(call, alAutenticar: null);
      var ahora = DateTime(2026, 10, 30, 9);
      final app = await abrirApp(tester, now: () => ahora);

      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      ahora = ahora.add(const Duration(seconds: 30));
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await esperar(tester);

      sesionCerradaEnElIngreso(app);
    });

    testWidgets('si volvió en menos de 10 segundos no pide la huella', (tester) async {
      respuesta = (call) => celularConHuella(call, alAutenticar: false);
      var ahora = DateTime(2026, 10, 30, 9);
      final app = await abrirApp(tester, now: () => ahora);

      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      ahora = ahora.add(const Duration(seconds: 5));
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await esperar(tester);

      expect(llamadas, isNot(contains('authenticate')));
      expect(app.read(authStateProvider).valueOrNull, isNotNull);
    });
  });
}
