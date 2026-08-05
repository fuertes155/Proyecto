// Smoke test de SecurityAlertApp: la pantalla de bloqueo que se muestra cuando se detecta
// un dispositivo comprometido (root/jailbreak). Es la única parte de main.dart que se puede
// probar sin ProviderScope ni mocks de red/sesión (MetApp sí los necesita — ver TODO abajo).
//
// TODO: agregar tests de MetApp() con ProviderScope(overrides: [...]) mockeando
// appRouterProvider/sessionProvider una vez existan providers de sesión fácilmente mockeables.
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
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
}
