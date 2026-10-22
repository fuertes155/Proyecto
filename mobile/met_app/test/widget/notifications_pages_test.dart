import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/intl.dart';
import 'package:met/features/auth/presentation/pages/notifications_settings_page.dart';
import 'package:met/features/notifications/presentation/pages/notifications_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

/// Bandeja de notificaciones y preferencias de contacto, contra un servidor falso.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter()
      ..on('GET', '/v1/notifications', const FakeResponse([
        {'id': 'n1', 'title': 'Abono recibido', 'message': 'Recibiste \$50.000', 'read': false, 'createdAt': '2026-10-21T15:00:00Z'},
        {'id': 'n2', 'title': 'Bienvenido', 'message': 'Hola', 'read': true, 'createdAt': '2026-10-01T10:00:00Z'},
      ]))
      ..on('GET', '/v1/notifications/unread-count', const FakeResponse({'count': 1}))
      ..on('PUT', '/v1/notifications/n1/read', const FakeResponse({}))
      ..on('PUT', '/v1/auth/notifications', const FakeResponse({
        'id': 'user-1', 'firstName': 'Ana', 'lastName': 'Pérez', 'status': 'ACTIVE',
        'emailNotificationsEnabled': false, 'pushNotificationsEnabled': true,
      }));
  });

  group('Bandeja', () {
    Future<void> openInbox(WidgetTester tester) async {
      await pumpScreen(tester, const NotificationsPage(), server: server);
      await tester.pumpAndSettle();
    }

    testWidgets('muestra las notificaciones con la hora local del socio', (tester) async {
      await openInbox(tester);

      expect(find.text('Abono recibido'), findsOneWidget);
      expect(find.text('Recibiste \$50.000'), findsOneWidget);
      final local = DateFormat('dd/MM/yyyy HH:mm').format(DateTime.parse('2026-10-21T15:00:00Z').toLocal());
      expect(find.text(local), findsOneWidget, reason: 'el backend envía la hora en UTC');
    });

    testWidgets('al tocar una no leída la marca como leída', (tester) async {
      await openInbox(tester);

      await tester.tap(find.text('Abono recibido'));
      await tester.pumpAndSettle();

      expect(server.requests.any((r) => r.method == 'PUT' && r.path == '/v1/notifications/n1/read'), isTrue);
      expect(find.byIcon(Icons.notifications_active), findsNothing);
    });

    testWidgets('tocar una ya leída no consulta al servidor', (tester) async {
      await openInbox(tester);

      await tester.tap(find.text('Bienvenido'));
      await tester.pumpAndSettle();

      expect(server.requests.where((r) => r.method == 'PUT'), isEmpty);
    });

    testWidgets('avisa cuando no hay notificaciones', (tester) async {
      server.on('GET', '/v1/notifications', const FakeResponse([]));
      await openInbox(tester);

      expect(find.text('No tienes notificaciones por el momento.'), findsOneWidget);
    });

    testWidgets('permite reintentar si no cargan', (tester) async {
      server.on('GET', '/v1/notifications', const FakeResponse({}, status: 500));
      await openInbox(tester);
      expect(find.text('Error al cargar notificaciones'), findsOneWidget);

      server.on('GET', '/v1/notifications', const FakeResponse([]));
      await tester.tap(find.text('Reintentar'));
      await tester.pumpAndSettle();

      expect(find.text('No tienes notificaciones por el momento.'), findsOneWidget);
    });
  });

  group('Preferencias', () {
    Future<void> openSettings(WidgetTester tester) async {
      await pumpScreen(tester, const NotificationsSettingsPage(),
          server: server,
          overrides: [
            signedIn(const {
              'id': 'user-1', 'firstName': 'Ana', 'lastName': 'Pérez', 'status': 'ACTIVE',
              'emailNotificationsEnabled': true, 'pushNotificationsEnabled': false,
            }),
          ]);
      await tester.pumpAndSettle();
    }

    bool switchValue(WidgetTester tester, String title) =>
        tester.widget<SwitchListTile>(find.widgetWithText(SwitchListTile, title)).value;

    testWidgets('parte de las preferencias actuales y guarda los cambios', (tester) async {
      await openSettings(tester);
      expect(switchValue(tester, 'Correos electrónicos'), isTrue);
      expect(switchValue(tester, 'Notificaciones Push'), isFalse);

      await tester.tap(find.text('Correos electrónicos'));
      await tester.tap(find.text('Notificaciones Push'));
      await tester.pump();
      await tester.tap(find.text('Guardar cambios'));
      await tester.pumpAndSettle();

      expect(server.last.json, {'emailNotificationsEnabled': false, 'pushNotificationsEnabled': true});
      expect(find.text('Preferencias actualizadas'), findsOneWidget);
    });

    testWidgets('explica el error sin texto técnico', (tester) async {
      server.on('PUT', '/v1/auth/notifications', const FakeResponse({}, status: 503));
      await openSettings(tester);

      await tester.tap(find.text('Guardar cambios'));
      await tester.pumpAndSettle();

      expect(find.text('Error en el servidor. Intenta más tarde.'), findsOneWidget);
    });
  });
}
