import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../helpers/fake_dio.dart';
import '../helpers/panel_harness.dart';

Map<String, dynamic> _alert(String id, String status, {String type = 'UNUSUAL_AMOUNT', String severity = 'HIGH'}) => {
      'id': id,
      'userId': 'u-$id',
      'userFullName': 'Laura Gómez',
      'userDocumentNumber': '1020456789',
      'transactionId': 'tx-$id',
      'alertType': type,
      'severity': severity,
      'description': 'Depósito de \$48.000.000, 12 veces su promedio',
      'status': status,
      'createdAt': '2026-10-21T15:00:00Z',
    };

/// Cumplimiento SARLAFT: revisión de alertas y coincidencias en listas OFAC/ONU.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    server = FakeDioAdapter()
      ..on('GET', '/v1/admin/compliance/alerts?status=OPEN&page=0&size=20',
          FakeResponse({'items': [_alert('a1', 'OPEN')], 'total': 1}))
      ..on('GET', '/v1/admin/compliance/alerts?status=REPORTED&page=0&size=20',
          FakeResponse({'items': [{..._alert('a2', 'REPORTED'), 'resolutionNotes': 'Enviado a la UIAF'}], 'total': 1}))
      ..on('POST', '/v1/admin/compliance/alerts/a1/review', FakeResponse(_alert('a1', 'DISMISSED')))
      ..on('GET', '/v1/admin/compliance/restrictive-list-matches?page=0&size=20', const FakeResponse([
        {'checkId': 'c1', 'userId': 'u9', 'userFullName': 'Pedro Pérez', 'userDocumentNumber': '79845112',
          'listType': 'OFAC', 'checkedAt': '2026-10-20T15:00:00Z'},
      ]))
      ..on('POST', '/v1/admin/compliance/restrictive-lists/refresh', const FakeResponse({'ofac': 1200, 'onu': 700}));
  });

  testWidgets('muestra las alertas abiertas con el socio y la operación', (tester) async {
    await pumpPanel(tester, '/admin/compliance', server: server);

    expect(find.text('1 alerta'), findsOneWidget);
    expect(find.text('Laura Gómez'), findsOneWidget);
    expect(find.text('Depósito de \$48.000.000, 12 veces su promedio'), findsOneWidget);
    expect(find.text('Poner en revisión'), findsOneWidget);
  });

  testWidgets('descartar una alerta guarda las notas y avisa aunque salga de la lista', (tester) async {
    await pumpPanel(tester, '/admin/compliance', server: server);
    // Ya revisada, deja de estar entre las abiertas (con la latencia de una red real).
    server.on('GET', '/v1/admin/compliance/alerts?status=OPEN&page=0&size=20',
        const FakeResponse({'items': [], 'total': 0}, delay: Duration(milliseconds: 300)));

    await tapAndSettle(tester, find.text('Descartar'));
    await tester.enterText(find.byType(TextField).last, 'Se validó el origen de los fondos');
    await tapAndSettle(tester, find.widgetWithText(FilledButton, 'Descartar'));

    expect(requestsTo(server, '/v1/admin/compliance/alerts/a1/review').single.json,
        {'status': 'DISMISSED', 'notes': 'Se validó el origen de los fondos'});
    expect(find.text('No hay alertas abiertas'), findsOneWidget);
    expect(find.text('Alerta actualizada'), findsOneWidget);
  });

  testWidgets('filtra por estado y muestra las notas de la resolución', (tester) async {
    await pumpPanel(tester, '/admin/compliance', server: server);

    await tapAndSettle(tester, find.text('Reportadas'));

    expect(find.text('Enviado a la UIAF'), findsOneWidget);
    expect(find.text('Descartar'), findsNothing, reason: 'una alerta reportada ya no se decide');
  });

  testWidgets('muestra las coincidencias en listas restrictivas y las actualiza', (tester) async {
    await pumpPanel(tester, '/admin/compliance', server: server);

    await tapAndSettle(tester, find.text('Listas restrictivas'));
    expect(find.text('Pedro Pérez'), findsOneWidget);
    expect(find.text('Coincide en OFAC'), findsOneWidget);

    await tapAndSettle(tester, find.text('Actualizar listas ahora'));
    expect(requestsTo(server, '/v1/admin/compliance/restrictive-lists/refresh'), hasLength(1));
    expect(find.text('Listas OFAC y ONU actualizadas'), findsOneWidget);
  });
}
