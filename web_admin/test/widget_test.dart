// Smoke tests de main.dart:
// - SecurityAlertApp: la pantalla de bloqueo que se muestra cuando se detecta un dispositivo
//   comprometido (root/jailbreak).
// - MetApp: dentro de su ProviderScope arranca en el login del administrador (el router real
//   redirige "/" a /admin/login y esa pantalla no toca red ni almacenamiento al construirse).
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:met_admin/main.dart';

void main() {
  setUp(() {
    // SecurityAlertApp llama a SystemNavigator.pop() (canal de plataforma) al tocar el botón;
    // en el entorno de test no hay plataforma real detrás del MethodChannel, así que lo simulamos.
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(SystemChannels.platform, (call) async => null);
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(SystemChannels.platform, null);
  });

  testWidgets('SecurityAlertApp muestra la alerta de seguridad y su botón de cierre',
      (WidgetTester tester) async {
    await tester.pumpWidget(const SecurityAlertApp());

    expect(find.text('Alerta de Seguridad'), findsOneWidget);
    expect(
      find.textContaining('dispositivo está modificado (Root/Jailbreak)'),
      findsOneWidget,
    );
    expect(find.byIcon(Icons.security), findsOneWidget);

    final closeButton = find.widgetWithText(ElevatedButton, 'Cerrar Aplicación');
    expect(closeButton, findsOneWidget);

    // No debe lanzar excepciones al tocar el botón (SystemNavigator.pop mockeado arriba).
    await tester.tap(closeButton);
    await tester.pumpAndSettle();
  });

  testWidgets('MetApp arranca en el login del administrador', (WidgetTester tester) async {
    await tester.pumpWidget(const ProviderScope(child: MetApp()));
    await tester.pumpAndSettle();

    expect(find.text('Bienvenido de nuevo'), findsOneWidget);
    expect(find.text('Usuario administrador'), findsOneWidget);
    expect(find.text('Contraseña'), findsOneWidget);
    expect(find.text('Iniciar sesión'), findsOneWidget);
  });

  testWidgets('El login valida los campos vacíos sin llamar al servidor', (WidgetTester tester) async {
    await tester.pumpWidget(const ProviderScope(child: MetApp()));
    await tester.pumpAndSettle();

    await tester.ensureVisible(find.text('Iniciar sesión'));
    await tester.tap(find.text('Iniciar sesión'));
    await tester.pumpAndSettle();

    expect(find.text('Escribe tu usuario de administrador'), findsOneWidget);
    expect(find.text('Escribe tu contraseña'), findsOneWidget);
  });

  testWidgets('El login usa dos columnas en pantallas anchas y una en angostas',
      (WidgetTester tester) async {
    addTearDown(tester.view.reset);

    tester.view.physicalSize = const Size(1440, 900);
    tester.view.devicePixelRatio = 1;
    await tester.pumpWidget(const ProviderScope(child: MetApp()));
    await tester.pumpAndSettle();
    expect(find.textContaining('El panel de MET para gestionar'), findsOneWidget);

    tester.view.physicalSize = const Size(400, 860);
    await tester.pumpAndSettle();
    expect(find.textContaining('El panel de MET para gestionar'), findsNothing);
    expect(find.text('Panel administrativo de la cooperativa'), findsOneWidget);
  });
}
