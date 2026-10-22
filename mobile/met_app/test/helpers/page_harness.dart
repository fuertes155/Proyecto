import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:met/core/network/api_client_provider.dart';
import 'package:met/core/theme/app_theme.dart';
import 'package:met/features/auth/data/models/auth_models.dart';
import 'package:met/features/auth/data/repositories/auth_repository_impl.dart';
import 'package:met/features/auth/presentation/providers/auth_provider.dart';

import 'fake_dio.dart';

/// Simula los plugins nativos que tocan las pantallas al abrirse, con el almacenamiento seguro
/// en memoria precargado con [storage].
///
/// - Acelerómetro (biometría conductual): un stream sin eventos.
/// - Huella (local_auth): el celular no tiene huella registrada.
/// - Fuente Outfit: el login fuerza el tema real; su descarga queda pendiente para siempre en vez
///   de fallar sin red (un error ahí rompe la prueba aunque la pantalla esté bien).
void mockNativePlugins({Map<String, String> storage = const {}}) {
  FlutterSecureStorage.setMockInitialValues(Map.of(storage));

  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  messenger
    ..setMockMethodCallHandler(const MethodChannel('dev.fluttercommunity.plus/sensors/method'), (_) async => null)
    ..setMockStreamHandler(const EventChannel('dev.fluttercommunity.plus/sensors/accelerometer'),
        MockStreamHandler.inline(onListen: (_, __) {}))
    ..setMockMethodCallHandler(const MethodChannel('plugins.flutter.io/local_auth'),
        (call) async => call.method == 'getAvailableBiometrics' ? <String>[] : false);

  GoogleFonts.config.httpClient = MockClient((_) => Completer<http.Response>().future);
}

/// Monta [screen] en un celular de 390×844 con la app real por debajo: providers, repositorios y
/// fuentes remotas reales contra [server]. Como en la app, la pantalla se abre encima de otra
/// ([previousScreen]), así que puede cerrarse con `pop`. Las rutas de [destinations] muestran
/// "Pantalla <ruta>" para comprobar a dónde navegó la pantalla.
Future<GoRouter> pumpScreen(
  WidgetTester tester,
  Widget screen, {
  required FakeDioAdapter server,
  Set<String> destinations = const {},
  List<Override> overrides = const [],
}) async {
  await tester.runAsync(() => _roboto ??= _loadRoboto());
  tester.view.physicalSize = const Size(390 * 3, 844 * 3);
  tester.view.devicePixelRatio = 3;
  addTearDown(tester.view.reset);

  final router = GoRouter(
    initialLocation: previousScreen,
    routes: [
      GoRoute(path: previousScreen, builder: (_, __) => const Scaffold(body: Text('Pantalla anterior'))),
      GoRoute(path: '/screen', builder: (_, __) => screen),
      for (final destination in destinations)
        GoRoute(path: destination, builder: (_, __) => Scaffold(body: Text('Pantalla $destination'))),
    ],
  );
  addTearDown(router.dispose);

  await tester.pumpWidget(ProviderScope(
    overrides: [apiClientProvider.overrideWithValue(fakeDio(server)), ...overrides],
    child: MaterialApp.router(theme: AppTheme.lightThemeWith((t) => t), routerConfig: router),
  ));
  router.push('/screen');
  // go_router resuelve la ruta de forma asíncrona: la pantalla aparece uno o dos cuadros después.
  for (var frame = 0; frame < 5 && find.byWidget(screen).evaluate().isEmpty; frame++) {
    await tester.pump();
  }
  return router;
}

Future<void>? _roboto;

/// Carga Roboto (la fuente por defecto de Flutter en Android) desde el SDK: con la fuente de
/// pruebas cada letra mide un cuadro completo y aparecen desbordes que en un celular no existen.
Future<void> _loadRoboto() async {
  final engine = File(Platform.resolvedExecutable).parent.parent.parent;
  final loader = FontLoader('Roboto');
  for (final weight in ['thin', 'light', 'regular', 'medium', 'bold', 'black']) {
    final bytes = File('${engine.path}/material_fonts/roboto-$weight.ttf').readAsBytesSync();
    loader.addFont(Future.value(ByteData.sublistView(bytes)));
  }
  await loader.load();
}

/// Sesión ya iniciada con [user] (el JSON de `/v1/auth/me`), como cuando la pantalla se abre desde
/// el inicio. El resto del notifier es el real (cerrar sesión, actualizar perfil...).
Override signedIn(Map<String, dynamic> user) =>
    authStateProvider.overrideWith((ref) => _SignedInAuthNotifier(ref, UserResponse.fromJson(user)));

class _SignedInAuthNotifier extends AuthNotifier {
  _SignedInAuthNotifier(Ref ref, UserResponse user) : super(ref.watch(authRepositoryProvider), ref) {
    state = AsyncValue.data(user);
  }
}

/// Ruta de la pantalla desde la que se abre la que está bajo prueba.
const previousScreen = '/previous';

/// Ruta en la que quedó la app (incluye las abiertas con `push`).
String currentPath(GoRouter router) => router.routerDelegate.currentConfiguration.last.matchedLocation;

/// `extra` con el que se abrió la ruta actual.
Object? currentExtra(GoRouter router) {
  final configuration = router.routerDelegate.currentConfiguration;
  final match = configuration.last;
  return match is ImperativeRouteMatch ? match.matches.extra : configuration.extra;
}
