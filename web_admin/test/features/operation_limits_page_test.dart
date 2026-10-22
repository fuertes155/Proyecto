import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met_admin/core/utils/currency_formatter.dart';

import '../helpers/fake_dio.dart';
import '../helpers/panel_harness.dart';

Map<String, dynamic> _limit(String type, int daily, int perTx) =>
    {'tipoOperacion': type, 'montoDiarioMax': daily, 'montoPorTransaccionMax': perTx, 'activo': true};

/// Topes de transferencias y retiros, contra un servidor falso.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    server = FakeDioAdapter()
      ..on('GET', '/v1/admin/limits', FakeResponse([
        _limit('TRANSFER', 5000000, 2000000),
        _limit('WITHDRAWAL', 3000000, 1000000),
        _limit('TRANSFER', 1, 1),
        _limit('PURCHASE', 900, 900),
      ]))
      ..on('PUT', '/v1/admin/limits', FakeResponse(_limit('TRANSFER', 6000000, 2500000)));
  });

  testWidgets('muestra un límite por tipo de operación con sus topes', (tester) async {
    await pumpPanel(tester, '/admin/limits', server: server);

    expect(find.text('Transferencias'), findsOneWidget, reason: 'el duplicado de TRANSFER no se repite');
    expect(find.text('Retiros'), findsOneWidget);
    expect(find.text(formatCop(5000000)), findsOneWidget);
    expect(find.text(formatCop(1000000)), findsOneWidget);
    expect(find.text(formatCop(900)), findsNothing, reason: 'PURCHASE no se administra aquí');
  });

  testWidgets('edita los topes y avisa que quedaron guardados', (tester) async {
    await pumpPanel(tester, '/admin/limits', server: server);

    await tapAndSettle(tester, find.text('Editar límites').first);
    final fields = find.byType(TextFormField);
    await tester.enterText(fields.at(0), '6000000');
    await tester.enterText(fields.at(1), '2500000');
    await tapAndSettle(tester, find.text('Guardar cambios'));

    expect(requestsTo(server, '/v1/admin/limits', method: 'PUT').single.json,
        {'tipoOperacion': 'TRANSFER', 'montoDiarioMax': 6000000, 'montoPorTransaccionMax': 2500000});
    expect(find.text('Límites de transferencias actualizados'), findsOneWidget);
  });

  testWidgets('no deja que el tope por transacción supere el diario ni montos en cero', (tester) async {
    await pumpPanel(tester, '/admin/limits', server: server);

    await tapAndSettle(tester, find.text('Editar límites').first);
    final fields = find.byType(TextFormField);
    await tester.enterText(fields.at(0), '1000000');
    await tester.enterText(fields.at(1), '2000000');
    await tapAndSettle(tester, find.text('Guardar cambios'));
    expect(find.text('No puede superar el máximo diario'), findsOneWidget);

    await tester.enterText(fields.at(0), '0');
    await tapAndSettle(tester, find.text('Guardar cambios'));
    expect(find.text('Debe ser mayor a cero'), findsOneWidget);

    expect(requestsTo(server, '/v1/admin/limits', method: 'PUT'), isEmpty);
  });

  testWidgets('muestra el error y permite reintentar', (tester) async {
    server.on('GET', '/v1/admin/limits', const FakeResponse({}, status: 500));
    await pumpPanel(tester, '/admin/limits', server: server);
    expect(find.text('Transferencias'), findsNothing);

    server.on('GET', '/v1/admin/limits', FakeResponse([_limit('WITHDRAWAL', 3000000, 1000000)]));
    await tapAndSettle(tester, find.text('Reintentar'));

    expect(find.text('Retiros'), findsOneWidget);
  });
}
