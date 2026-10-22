import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met_admin/core/utils/currency_formatter.dart';

import '../helpers/fake_dio.dart';
import '../helpers/panel_harness.dart';

Map<String, dynamic> _rule(String id, String name, String condition, String action, {bool active = true}) => {
      'id': id,
      'nombre': name,
      'descripcion': 'Regla $name',
      'condicion': condition,
      'accion': action,
      'activo': active,
    };

Map<String, dynamic> _window(String id, String description, {bool active = false}) => {
      'id': id,
      'descripcion': description,
      'inicio': '2026-10-25T08:00:00Z',
      'fin': '2026-10-25T10:00:00Z',
      'activo': active,
    };

/// Reglas antifraude y ventanas de mantenimiento: activar y pausar con confirmación.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    server = FakeDioAdapter()
      ..on('GET', '/v1/admin/risk-rules', FakeResponse([
        _rule('r1', 'Monto alto', '{"field":"amount","operator":"gt","value":10000000}', 'REVIEW'),
        _rule('r2', 'Fuerza bruta', '{"field":"failed_attempts","operator":"gte","value":5,"window_minutes":10}', 'BLOCK',
            active: false),
        _rule('r3', 'Personalizada', 'texto libre', 'NOTIFY'),
      ]))
      ..on('PATCH', '/v1/admin/risk-rules/r1/toggle?activo=false', const FakeResponse({}))
      ..on('PATCH', '/v1/admin/risk-rules/r2/toggle?activo=true', const FakeResponse({}))
      ..on('GET', '/v1/admin/maintenance', FakeResponse([_window('m1', 'Actualización del core')]))
      ..on('PATCH', '/v1/admin/maintenance/m1/activate', const FakeResponse({}));
  });

  group('Reglas de riesgo', () {
    testWidgets('describe cada condición y acción en lenguaje claro', (tester) async {
      await pumpPanel(tester, '/admin/risk-rules', server: server);

      expect(find.text('2 de 3 reglas activas revisando o bloqueando operaciones en tiempo real.'), findsOneWidget);
      expect(find.text('Monto mayor a ${formatCop(10000000)}'), findsOneWidget);
      expect(find.text('Intentos fallidos mayor o igual a 5 en 10 minutos'), findsOneWidget);
      expect(find.text('texto libre'), findsOneWidget, reason: 'una condición que no es JSON se muestra tal cual');
      expect(find.text('Revisión manual'), findsOneWidget);
      expect(find.text('Bloqueo automático'), findsOneWidget);
    });

    testWidgets('pausar una regla pide confirmación y avisa el resultado', (tester) async {
      await pumpPanel(tester, '/admin/risk-rules', server: server);
      // Con una red real, la recarga posterior tarda y la pantalla se dibuja mientras tanto.
      server.slowDown('GET', '/v1/admin/risk-rules', const Duration(milliseconds: 300));

      await tapAndSettle(tester, find.byType(Switch).first);
      expect(find.text('Pausar regla'), findsWidgets);
      await tapAndSettle(tester, find.widgetWithText(FilledButton, 'Pausar regla'));

      expect(requestsTo(server, '/v1/admin/risk-rules/r1/toggle?activo=false'), hasLength(1));
      expect(find.text('Regla "Monto alto" pausada'), findsOneWidget);
    });

    testWidgets('activar una regla no pide confirmación', (tester) async {
      await pumpPanel(tester, '/admin/risk-rules', server: server);
      // Con una red real, la recarga posterior tarda y la pantalla se dibuja mientras tanto.
      server.slowDown('GET', '/v1/admin/risk-rules', const Duration(milliseconds: 300));

      await tapAndSettle(tester, find.byType(Switch).at(1));

      expect(requestsTo(server, '/v1/admin/risk-rules/r2/toggle?activo=true'), hasLength(1));
      expect(find.text('Regla "Fuerza bruta" activada'), findsOneWidget);
    });
  });

  group('Mantenimiento', () {
    testWidgets('activar una ventana pide confirmación y avisa a los socios', (tester) async {
      await pumpPanel(tester, '/admin/maintenance', server: server);
      server.slowDown('GET', '/v1/admin/maintenance', const Duration(milliseconds: 300));
      expect(find.text('Actualización del core'), findsOneWidget);

      await tapAndSettle(tester, find.byType(Switch));
      await tapAndSettle(tester, find.text('Activar ahora'));

      expect(requestsTo(server, '/v1/admin/maintenance/m1/activate'), hasLength(1));
      expect(find.text('Mantenimiento activado'), findsOneWidget);
    });

    testWidgets('advierte cuando hay un mantenimiento activo', (tester) async {
      server.on('GET', '/v1/admin/maintenance', FakeResponse([_window('m1', 'Migración', active: true)]));
      await pumpPanel(tester, '/admin/maintenance', server: server);

      expect(find.text('Hay un mantenimiento activo'), findsOneWidget);
    });
  });
}
