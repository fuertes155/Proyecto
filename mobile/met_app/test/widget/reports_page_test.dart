import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/intl.dart';
import 'package:met/features/reports/presentation/pages/reports_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

/// Reporte descargable (PDF o Excel) del periodo elegido, compartido con la hoja del sistema.
void main() {
  late FakeDioAdapter server;
  late Directory temp;
  late List<Map<Object?, Object?>> shared;

  setUpAll(() => temp = Directory.systemTemp.createTempSync('met_reports_'));
  tearDownAll(() => temp.deleteSync(recursive: true));

  setUp(() {
    mockNativePlugins();
    shared = [];
    final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
    messenger
      ..setMockMethodCallHandler(const MethodChannel('plugins.flutter.io/path_provider'), (_) async => temp.path)
      ..setMockMethodCallHandler(const MethodChannel('dev.fluttercommunity.plus/share'), (call) async {
        shared.add(call.arguments as Map<Object?, Object?>);
        return 'dev.fluttercommunity.plus/share/success';
      });
    server = FakeDioAdapter()..on('GET', '/v1/reports/export', const FakeResponse('%PDF-1.7 reporte'));
  });

  Future<void> openReports(WidgetTester tester) async {
    await pumpScreen(tester, const ReportsPage(), server: server);
    await tester.pumpAndSettle();
  }

  /// Escribir el archivo es entrada/salida real: se le da tiempo real y luego se procesa lo pendiente.
  Future<void> generate(WidgetTester tester) async {
    await tester.ensureVisible(find.text('Generar y compartir'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Generar y compartir'));
    for (var i = 0; i < 10; i++) {
      await tester.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 20)));
      await tester.pump(const Duration(milliseconds: 100));
    }
    await tester.pumpAndSettle();
  }

  String ymd(DateTime date) => DateFormat('yyyy-MM-dd').format(date);
  String compact(DateTime date) => DateFormat('yyyyMMdd').format(date);

  testWidgets('genera el PDF del mes en curso y lo comparte', (tester) async {
    final now = DateTime.now();
    await openReports(tester);

    await generate(tester);

    final firstOfMonth = DateTime(now.year, now.month, 1);
    expect(server.last.queryParameters, {'from': ymd(firstOfMonth), 'to': ymd(now), 'format': 'pdf'});
    final path = (shared.single['paths']! as List).single as String;
    expect(path, endsWith('reporte_${compact(firstOfMonth)}_a_${compact(now)}.pdf'));
    expect(shared.single['mimeTypes'], ['application/pdf']);
    expect(File(path).readAsStringSync(), '%PDF-1.7 reporte');
  });

  testWidgets('genera el Excel del año en curso', (tester) async {
    final now = DateTime.now();
    await openReports(tester);

    await tester.tap(find.text('Este año'));
    await tester.tap(find.text('Excel'));
    await tester.pump();
    await generate(tester);

    expect(server.last.queryParameters, {'from': ymd(DateTime(now.year, 1, 1)), 'to': ymd(now), 'format': 'xlsx'});
    expect((shared.single['paths']! as List).single as String, endsWith('.xlsx'));
    expect(shared.single['mimeTypes'], ['application/vnd.openxmlformats-officedocument.spreadsheetml.sheet']);
  });

  testWidgets('muestra el motivo si el backend no puede generar el reporte', (tester) async {
    server.on('GET', '/v1/reports/export',
        const FakeResponse({'code': 'REPORT_TOO_LARGE', 'message': 'El periodo es demasiado largo'}, status: 422));
    await openReports(tester);

    await generate(tester);

    expect(find.text('El periodo es demasiado largo'), findsOneWidget);
    expect(shared, isEmpty);
  });
}
