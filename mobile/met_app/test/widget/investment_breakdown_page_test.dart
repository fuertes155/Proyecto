import 'package:flutter_test/flutter_test.dart';
import 'package:intl/intl.dart';
import 'package:met/features/investment/presentation/pages/investment_breakdown_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

Map<String, dynamic> _summary({
  double totalInvested = 1200000,
  double active = 800000,
  double available = 200000,
  double paidOff = 150000,
  double returned = 50000,
  int loansFunded = 3,
}) =>
    {
      'totalInvested': totalInvested,
      'activeAmount': active,
      'availableAmount': available,
      'paidOffAmount': paidOff,
      'returnedAmount': returned,
      'loansFundedCount': loansFunded,
    };

/// Resumen de en qué está trabajando el capital del socio, sin mostrar a quién financia.
void main() {
  late FakeDioAdapter server;
  final currency = NumberFormat.currency(locale: 'es_CO', symbol: '\$', decimalDigits: 0);

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter()..on('GET', '/v1/investments/my-summary', FakeResponse(_summary()));
  });

  Future<void> openInvestments(WidgetTester tester) async {
    await pumpScreen(tester, const InvestmentBreakdownPage(), server: server);
    await tester.pumpAndSettle();
  }

  testWidgets('desglosa el capital en activo, disponible, recuperado y cubierto', (tester) async {
    await openInvestments(tester);

    expect(find.text(currency.format(1200000)), findsOneWidget);
    expect(find.text('Generando rendimiento'), findsOneWidget);
    expect(find.text('Financiando a 3 socio(s) de la cooperativa'), findsOneWidget);
    expect(find.text(currency.format(800000)), findsOneWidget);
    expect(find.text('Disponible por asignar'), findsOneWidget);
    expect(find.text('Recuperado'), findsOneWidget);
    expect(find.text('Cubierto por el fondo de garantías'), findsOneWidget);
  });

  testWidgets('omite las partes en cero', (tester) async {
    server.on('GET', '/v1/investments/my-summary',
        FakeResponse(_summary(available: 0, paidOff: 0, returned: 0, loansFunded: 0)));
    await openInvestments(tester);

    expect(find.text('Activo en la cooperativa'), findsOneWidget);
    expect(find.text('Disponible por asignar'), findsNothing);
    expect(find.text('Recuperado'), findsNothing);
    expect(find.text('Cubierto por el fondo de garantías'), findsNothing);
  });

  testWidgets('sin dinero invertido explica qué verá aquí', (tester) async {
    server.on('GET', '/v1/investments/my-summary', FakeResponse(_summary(totalInvested: 0)));
    await openInvestments(tester);

    expect(find.text('Aún no tienes dinero trabajando'), findsOneWidget);
  });

  testWidgets('permite reintentar si no carga', (tester) async {
    server.on('GET', '/v1/investments/my-summary', const FakeResponse({}, status: 500));
    await openInvestments(tester);
    expect(find.text('No se pudo cargar tu inversión.'), findsOneWidget);

    server.on('GET', '/v1/investments/my-summary', FakeResponse(_summary()));
    await tester.tap(find.text('Reintentar'));
    await tester.pumpAndSettle();

    expect(find.text('Generando rendimiento'), findsOneWidget);
  });
}
