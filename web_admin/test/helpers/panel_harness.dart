import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met_admin/core/network/api_client_provider.dart';
import 'package:met_admin/core/router/app_router.dart';
import 'package:met_admin/core/theme/app_theme.dart';
import 'package:met_admin/features/admin/providers/admin_business_provider.dart';

import 'fake_dio.dart';

/// Abre [route] del panel en una pantalla de escritorio con la app real (router, menú lateral,
/// providers y repositorios) contra [server], con la sesión de administrador ya iniciada.
///
/// Las dos APIs del panel llegan al mismo servidor falso: la de administración por la ruta completa
/// (`/v1/admin/limits`) y la de negocio relativa a `/v1/admin` (`/users`, `/loans`, `/accounts`).
Future<ProviderContainer> pumpPanel(WidgetTester tester, String route, {required FakeDioAdapter server}) async {
  await tester.runAsync(() => _fonts ??= _loadFonts());
  FlutterSecureStorage.setMockInitialValues({'admin_access_token': 'admin-token'});
  tester.view.physicalSize = const Size(1440, 1000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  final container = ProviderContainer(overrides: [
    apiClientProvider.overrideWithValue(fakeDio(server)),
    adminApiClientProvider.overrideWithValue(fakeDio(server, baseUrl: 'http://api.test/api/v1/admin')),
  ]);
  addTearDown(container.dispose);

  final router = container.read(appRouterProvider);
  await tester.pumpWidget(UncontrolledProviderScope(
    container: container,
    child: MaterialApp.router(theme: AppTheme.lightTheme, routerConfig: router),
  ));
  router.go(route);
  await tester.pumpAndSettle();
  return container;
}

Future<void>? _fonts;

/// El panel usa Outfit (google_fonts), que en las pruebas no se descarga: sin una fuente real cada
/// letra mide un cuadro completo y aparecen desbordes que en el navegador no existen. Se registra
/// Roboto del SDK con los nombres que pide google_fonts, por peso.
Future<void> _loadFonts() async {
  final fonts = '${File(Platform.resolvedExecutable).parent.parent.parent.path}/material_fonts';
  ByteData roboto(String weight) => ByteData.sublistView(File('$fonts/roboto-$weight.ttf').readAsBytesSync());
  const families = {
    'Outfit_regular': 'regular',
    'Outfit_500': 'medium',
    'Outfit_600': 'medium',
    'Outfit_700': 'bold',
    'Outfit_800': 'black',
    'Roboto': 'regular',
  };
  for (final MapEntry(key: family, value: weight) in families.entries) {
    await (FontLoader(family)..addFont(Future.value(roboto(weight)))).load();
  }
}

/// Ruta en la que quedó el panel.
String panelPath(ProviderContainer container) =>
    container.read(appRouterProvider).routerDelegate.currentConfiguration.uri.path;

/// Toca [finder] (desplazándose hasta él si hace falta) y espera a que termine todo.
Future<void> tapAndSettle(WidgetTester tester, Finder finder) async {
  await tester.ensureVisible(finder);
  await tester.pumpAndSettle();
  await tester.tap(finder);
  await tester.pumpAndSettle();
}

/// Peticiones que llegaron a [path] (por ejemplo, para revisar el cuerpo enviado).
List<RecordedRequest> requestsTo(FakeDioAdapter server, String path, {String? method}) => server.requests
    .where((r) => r.path == path && (method == null || r.method == method))
    .toList();
