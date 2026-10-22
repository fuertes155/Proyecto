import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met_admin/core/utils/currency_formatter.dart';

import '../helpers/fake_dio.dart';
import '../helpers/panel_harness.dart';

Map<String, dynamic> _fee(String id, String code, double value, {bool percent = true, String? until}) => {
      'id': id,
      'tipoTarifa': code,
      'descripcion': 'Cobro de $code',
      'valor': value,
      'esPorcentaje': percent,
      'vigentDesde': '2026-10-01T15:00:00Z',
      'vigentaHasta': until,
    };

/// Tarifas y comisiones: listado, creación, edición y eliminación contra un servidor falso.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    server = FakeDioAdapter()
      ..on('GET', '/v1/admin/fees', FakeResponse([
        _fee('f1', 'TRANSFERENCIA_EXTERNA', 0.015),
        _fee('f2', 'RETIRO', 3500, percent: false),
      ]))
      ..on('POST', '/v1/admin/fees', FakeResponse(_fee('f3', 'RECARGA_PSE', 0.025)))
      ..on('PUT', '/v1/admin/fees/f1', FakeResponse(_fee('f1', 'TRANSFERENCIA_EXTERNA', 0.02)))
      ..on('DELETE', '/v1/admin/fees/f2', const FakeResponse({}));
  });

  Future<void> fillForm(WidgetTester tester, {String? code, required String value, bool fixed = false}) async {
    final fields = find.byType(TextFormField);
    if (code != null) await tester.enterText(fields.at(0), code);
    if (fixed) await tapAndSettle(tester, find.text('Valor fijo').last);
    await tester.enterText(fields.at(2), value);
    await tester.pump();
  }

  testWidgets('muestra cada tarifa legible: porcentaje o valor fijo', (tester) async {
    await pumpPanel(tester, '/admin/fees', server: server);

    expect(find.text('Transferencia externa'), findsOneWidget);
    expect(find.text('1.50 %'), findsOneWidget);
    expect(find.text(formatCop(3500)), findsOneWidget);
    expect(find.text('Desde 01/10/2026'), findsNWidgets(2));
  });

  testWidgets('crea una tarifa en porcentaje guardándola como fracción', (tester) async {
    await pumpPanel(tester, '/admin/fees', server: server);

    await tapAndSettle(tester, find.text('Nueva tarifa').first);
    await fillForm(tester, code: 'recarga pse', value: '2,5');
    await tapAndSettle(tester, find.text('Crear tarifa'));

    final sent = requestsTo(server, '/v1/admin/fees', method: 'POST').single.json;
    expect(sent['tipoTarifa'], 'RECARGA_PSE');
    expect(sent['valor'], 0.025);
    expect(sent['esPorcentaje'], isTrue);
    expect(DateTime.tryParse(sent['vigentDesde'] as String), isNotNull);
    expect(find.text('Tarifa creada'), findsOneWidget);
  });

  testWidgets('valida el formulario antes de enviar', (tester) async {
    await pumpPanel(tester, '/admin/fees', server: server);

    await tapAndSettle(tester, find.text('Nueva tarifa').first);
    await fillForm(tester, value: '150');
    await tapAndSettle(tester, find.text('Crear tarifa'));

    expect(find.text('Escribe el código de la tarifa'), findsOneWidget);
    expect(find.text('Un porcentaje no puede superar 100'), findsOneWidget);
    expect(requestsTo(server, '/v1/admin/fees', method: 'POST'), isEmpty);
  });

  testWidgets('edita una tarifa partiendo de sus valores actuales', (tester) async {
    await pumpPanel(tester, '/admin/fees', server: server);

    await tapAndSettle(tester, find.byTooltip('Editar tarifa').first);
    expect(find.widgetWithText(TextFormField, '1.5'), findsOneWidget);
    await fillForm(tester, value: '2');
    await tapAndSettle(tester, find.text('Guardar cambios'));

    final sent = requestsTo(server, '/v1/admin/fees/f1', method: 'PUT').single.json;
    expect(sent['tipoTarifa'], 'TRANSFERENCIA_EXTERNA');
    expect(sent['valor'], 0.02);
    expect(find.text('Tarifa actualizada'), findsOneWidget);
  });

  testWidgets('elimina una tarifa solo después de confirmar', (tester) async {
    await pumpPanel(tester, '/admin/fees', server: server);

    await tapAndSettle(tester, find.byTooltip('Eliminar tarifa').last);
    await tapAndSettle(tester, find.text('Cancelar'));
    expect(requestsTo(server, '/v1/admin/fees/f2', method: 'DELETE'), isEmpty);

    await tapAndSettle(tester, find.byTooltip('Eliminar tarifa').last);
    await tapAndSettle(tester, find.text('Eliminar'));
    expect(requestsTo(server, '/v1/admin/fees/f2', method: 'DELETE'), hasLength(1));
    expect(find.text('Tarifa eliminada'), findsOneWidget);
  });

  testWidgets('sin tarifas invita a crear la primera', (tester) async {
    server.on('GET', '/v1/admin/fees', const FakeResponse([]));
    await pumpPanel(tester, '/admin/fees', server: server);

    expect(find.text('No hay tarifas configuradas'), findsOneWidget);
  });
}
