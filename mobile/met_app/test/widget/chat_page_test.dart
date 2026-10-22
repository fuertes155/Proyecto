import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met/features/auth/presentation/providers/auth_provider.dart';
import 'package:met/features/support/presentation/pages/chat_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

/// El asistente de soporte: opciones rápidas, preguntas al backend y respuestas de respaldo.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter()
      ..on('POST', '/support/chat', const FakeResponse({'reply': 'Tu saldo disponible es \$ 200.000'}));
  });

  Future<void> openChat(WidgetTester tester) async {
    await pumpScreen(tester, const ChatPage(),
        server: server,
        overrides: [
          signedIn(const {
            'id': 'user-1', 'firstName': 'Ana', 'lastName': 'Pérez', 'email': 'socio@example.com', 'status': 'ACTIVE',
          }),
        ]);
    await tester.pumpAndSettle();
  }

  Future<void> ask(WidgetTester tester, String message) async {
    await tester.enterText(find.byType(TextField), message);
    await tester.tap(find.byTooltip('Enviar'));
    await tester.pumpAndSettle();
  }

  testWidgets('responde una opción rápida y oculta las demás', (tester) async {
    await openChat(tester);
    expect(find.text('Soy el asistente virtual de MET. ¿En qué te puedo ayudar?'), findsOneWidget);

    await tester.tap(find.text('📞 Contacto'));
    await tester.pump(const Duration(milliseconds: 400)); // la respuesta enlatada llega tras una pausa
    await tester.pumpAndSettle();

    expect(find.textContaining('atencion@met.coop'), findsOneWidget);
    expect(find.text('💰 Saldo'), findsNothing);
    expect(server.requests, isEmpty, reason: 'las opciones rápidas no consultan al servidor');
  });

  testWidgets('envía la pregunta al asistente y muestra su respuesta', (tester) async {
    await openChat(tester);

    await ask(tester, '¿Cuánto tengo?');

    expect(server.last.json, {'message': '¿Cuánto tengo?'});
    expect(find.text('¿Cuánto tengo?'), findsOneWidget);
    expect(find.text('Tu saldo disponible es \$ 200.000'), findsOneWidget);
    expect(tester.widget<TextField>(find.byType(TextField)).controller!.text, isEmpty);
  });

  testWidgets('si el asistente no responde, contesta por palabra clave', (tester) async {
    server.on('POST', '/support/chat', const FakeResponse({}, status: 503));
    await openChat(tester);

    await ask(tester, 'quiero un préstamo');

    expect(find.textContaining('Ve a Créditos en el menú'), findsOneWidget);
  });

  testWidgets('ignora los mensajes vacíos', (tester) async {
    await openChat(tester);

    await ask(tester, '   ');

    expect(server.requests, isEmpty);
    expect(find.text('💰 Saldo'), findsOneWidget);
  });

  testWidgets('al cerrar sesión la conversación no queda para el siguiente socio', (tester) async {
    await openChat(tester);
    await ask(tester, 'Mi cédula es 1086578123');
    final container = ProviderScope.containerOf(tester.element(find.byType(ChatPage)));

    await container.read(authStateProvider.notifier).forceLocalLogout();
    await tester.pumpAndSettle();

    expect(find.text('Mi cédula es 1086578123'), findsNothing);
    expect(find.text('💰 Saldo'), findsOneWidget);
  });
}
