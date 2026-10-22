import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:met/core/utils/currency_formatter.dart';
import 'package:met/features/transfers/presentation/pages/account_statement_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

Map<String, dynamic> _movement(String id, String type, String label, double amount, {bool credit = false, String concept = ''}) => {
      'id': id,
      'type': type,
      'typeLabel': label,
      'concept': concept,
      'amount': amount,
      'isCredit': credit,
      'status': 'COMPLETED',
      'createdAt': '2026-10-20T15:30:00Z',
    };

/// Movimientos del socio y extracto mensual, contra un servidor falso.
void main() {
  late FakeDioAdapter server;

  setUpAll(() => initializeDateFormatting('es_CO'));

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter()
      ..on('GET', '/v1/accounts/movements', FakeResponse([
        _movement('m1', 'DEPOSIT', 'Recarga', 500000, credit: true, concept: 'PSE Bancolombia'),
        _movement('m2', 'EXTERNAL_PAYOUT', 'Retiro a banco', 150000),
      ]))
      ..on('GET', '/v1/accounts/statement', const FakeResponse(
          'Fecha,Tipo,Descripcion,Monto,Direccion\n'
          '2026-10-01 09:00,DEPOSIT,"Recarga, PSE",500000,CREDITO\n'
          "2026-10-05 12:00,TRANSFER,'-Pago arriendo,120000,DEBITO\n"));
  });

  Future<void> openStatement(WidgetTester tester) async {
    await pumpScreen(tester, const AccountStatementPage(), server: server);
    await tester.pumpAndSettle();
  }

  Future<void> tapAndSettle(WidgetTester tester, Finder finder) async {
    await tester.ensureVisible(finder);
    await tester.pumpAndSettle();
    await tester.tap(finder);
    await tester.pumpAndSettle();
  }

  testWidgets('lista los movimientos con su signo y concepto', (tester) async {
    await openStatement(tester);

    expect(find.text('Recarga'), findsOneWidget);
    expect(find.textContaining('PSE Bancolombia'), findsOneWidget);
    expect(find.text('+${formatCop(500000)}'), findsOneWidget);
    expect(find.text('Retiro a banco'), findsOneWidget);
    expect(find.text('-${formatCop(150000)}'), findsOneWidget);
  });

  testWidgets('avisa cuando aún no hay movimientos', (tester) async {
    server.on('GET', '/v1/accounts/movements', const FakeResponse([]));
    await openStatement(tester);

    expect(find.text('Todavía no tienes movimientos registrados.'), findsOneWidget);
  });

  testWidgets('permite reintentar si los movimientos no cargan', (tester) async {
    server.on('GET', '/v1/accounts/movements', const FakeResponse({}, status: 500));
    await openStatement(tester);
    expect(find.text('No se pudo cargar la información. Intenta de nuevo.'), findsOneWidget);

    server.on('GET', '/v1/accounts/movements', const FakeResponse([]));
    await tapAndSettle(tester, find.text('Reintentar'));

    expect(find.text('Todavía no tienes movimientos registrados.'), findsOneWidget);
  });

  testWidgets('genera el extracto del mes elegido y lo muestra legible', (tester) async {
    await openStatement(tester);

    await tapAndSettle(tester, find.text('Descargar extracto mensual'));
    await tapAndSettle(tester, find.text('Generar extracto'));

    final now = DateTime.now();
    expect(server.last.queryParameters, {'year': now.year, 'month': now.month});
    expect(find.text('2 movimiento(s) en ${now.month}/${now.year}'), findsOneWidget);
    expect(find.text('Recarga, PSE'), findsOneWidget, reason: 'la coma dentro de comillas no parte la columna');
    expect(find.text('-Pago arriendo'), findsOneWidget,
        reason: "el apóstrofo que protege el CSV de fórmulas en Excel no se muestra en la app");
    expect(find.text('-${formatCop(120000)}'), findsOneWidget);
  });

  testWidgets('explica si no hay movimientos en el periodo o si falla', (tester) async {
    server.on('GET', '/v1/accounts/statement', const FakeResponse('Fecha,Tipo,Descripcion,Monto,Direccion\n'));
    await openStatement(tester);
    await tapAndSettle(tester, find.text('Descargar extracto mensual'));

    await tapAndSettle(tester, find.text('Generar extracto'));
    expect(find.text('No hay movimientos en este periodo.'), findsOneWidget);

    server.on('GET', '/v1/accounts/statement', const FakeResponse('error', status: 500));
    await tapAndSettle(tester, find.text('Generar extracto'));
    expect(find.text('No se pudo cargar la información. Intenta de nuevo.'), findsOneWidget);
  });
}
