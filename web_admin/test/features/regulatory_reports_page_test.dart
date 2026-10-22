import 'package:flutter_test/flutter_test.dart';

import '../helpers/fake_dio.dart';
import '../helpers/panel_harness.dart';

Map<String, dynamic> _report(String id, String type, String status, {int month = 9, int? records}) => {
      'id': id,
      'reportType': type,
      'periodYear': 2026,
      'periodMonth': month,
      'status': status,
      'fileName': status == 'COMPLETED' ? '${type.toLowerCase()}_2026$month.txt' : null,
      'fileSizeBytes': status == 'COMPLETED' ? 2048 : null,
      'recordCount': records,
      'generatedAt': '2026-10-01T15:00:00Z',
    };

/// Reportes planos para la Supersolidaria: generar por periodo e historial descargable.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    server = FakeDioAdapter()
      ..on('GET', '/v1/compliance/reports', FakeResponse([
        _report('r1', 'ASOCIADOS', 'COMPLETED', records: 1250),
        _report('r2', 'SARLAFT', 'FAILED'),
      ]))
      ..on('POST', '/v1/compliance/reports/generate', FakeResponse(_report('r3', 'SARLAFT', 'COMPLETED', records: 1250)))
      ..on('GET', '/v1/compliance/reports/r1/download', const FakeResponse('0001|1020456789|LAURA'));
  });

  testWidgets('genera por defecto el periodo anterior del tipo elegido', (tester) async {
    final previous = DateTime(DateTime.now().year, DateTime.now().month - 1);
    await pumpPanel(tester, '/admin/reports', server: server);

    await tapAndSettle(tester, find.text('Verificaciones SARLAFT').first);
    await tapAndSettle(tester, find.text('Generar reporte'));

    expect(requestsTo(server, '/v1/compliance/reports/generate').single.json,
        {'reportType': 'SARLAFT', 'periodYear': previous.year, 'periodMonth': previous.month});
    expect(find.textContaining('(1.250 registros)'), findsOneWidget);
    expect(requestsTo(server, '/v1/compliance/reports'), hasLength(2), reason: 'recarga el historial');
  });

  testWidgets('muestra el historial y solo deja descargar los reportes listos', (tester) async {
    await pumpPanel(tester, '/admin/reports', server: server);

    expect(find.text('Septiembre 2026'), findsNWidgets(2));
    expect(find.text('Listo'), findsOneWidget);
    expect(find.text('Falló'), findsOneWidget);
    expect(find.text('Descargar'), findsOneWidget);

    await tapAndSettle(tester, find.text('Descargar'));
    expect(requestsTo(server, '/v1/compliance/reports/r1/download'), hasLength(1));
  });

  testWidgets('muestra el error si el backend no puede generar el reporte', (tester) async {
    server.on('POST', '/v1/compliance/reports/generate',
        const FakeResponse({'code': 'REPORT_FAILED', 'message': 'No hay datos para ese periodo'}, status: 422));
    await pumpPanel(tester, '/admin/reports', server: server);

    await tapAndSettle(tester, find.text('Generar reporte'));

    expect(find.text('No hay datos para ese periodo'), findsOneWidget);
  });

  testWidgets('sin historial invita a generar el primero', (tester) async {
    server.on('GET', '/v1/compliance/reports', const FakeResponse([]));
    await pumpPanel(tester, '/admin/reports', server: server);

    expect(find.text('Todavía no has generado reportes'), findsOneWidget);
  });
}
