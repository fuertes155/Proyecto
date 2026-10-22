import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/features/transfers/presentation/pages/deposit_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

/// Recarga de la cuenta por la pasarela de Wompi (Nequi, PSE...) contra un servidor falso.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter()
      ..on('GET', '/v1/accounts/me', const FakeResponse({
        'id': 'acc-1', 'accountNumber': '4510009283', 'principalBalance': 2450000, 'interestBalance': 200000,
        'status': 'ACTIVE',
      }))
      ..on('POST', '/v1/accounts/deposit-pse', const FakeResponse({'paymentUrl': 'https://checkout.wompi.co/l/abc123'}))
      ..on('GET', '/v1/banks', const FakeResponse([
        {'code': '1007', 'name': 'Bancolombia', 'supportsPayout': true},
        {'code': '1051', 'name': 'Banco Davivienda'},
      ]));
  });

  Future<GoRouter> openDeposit(WidgetTester tester, {String method = 'Nequi', String? bankHint}) async {
    final router = await pumpScreen(tester, DepositPage(method: method, bankHint: bankHint),
        server: server, destinations: {'/deposit/webview', '/home'});
    await tester.pumpAndSettle();
    return router;
  }

  Future<void> tapAndSettle(WidgetTester tester, Finder finder) async {
    await tester.ensureVisible(finder);
    await tester.pumpAndSettle();
    await tester.tap(finder);
    await tester.pumpAndSettle();
  }

  List<RecordedRequest> depositRequests() => server.requests.where((r) => r.path == '/v1/accounts/deposit-pse').toList();

  testWidgets('elige un monto rápido y abre la pasarela con el enlace de pago', (tester) async {
    final router = await openDeposit(tester);

    await tapAndSettle(tester, find.text('\$50.000'));
    await tapAndSettle(tester, find.text('DEPÓSITO'));

    expect(depositRequests().single.json, {'amount': 50000.0, 'returnUrl': 'metapp://deposit/success'});
    expect(currentPath(router), '/deposit/webview');
    expect(currentExtra(router), {
      'paymentUrl': 'https://checkout.wompi.co/l/abc123',
      'method': 'Nequi',
      'amount': 50000.0,
    });
  });

  testWidgets('ignora los separadores de miles que escriba el socio', (tester) async {
    await openDeposit(tester);

    await tester.enterText(find.byType(TextField), '150.000');
    await tester.pumpAndSettle();
    await tapAndSettle(tester, find.text('DEPÓSITO'));

    expect(depositRequests().single.json['amount'], 150000.0);
  });

  testWidgets('arma el enlace completo cuando el backend responde una ruta relativa', (tester) async {
    server.on('POST', '/v1/accounts/deposit-pse', const FakeResponse({'paymentUrl': '/v1/payments/checkout/abc123'}));
    final router = await openDeposit(tester);

    await tapAndSettle(tester, find.text('\$20.000'));
    await tapAndSettle(tester, find.text('DEPÓSITO'));

    expect((currentExtra(router)! as Map)['paymentUrl'], 'http://api.test/api/v1/payments/checkout/abc123');
  });

  testWidgets('pide el monto antes de depositar', (tester) async {
    await openDeposit(tester);

    await tapAndSettle(tester, find.text('DEPÓSITO'));

    expect(find.text('Ingresa un monto para depositar'), findsOneWidget);
    expect(depositRequests(), isEmpty);
  });

  testWidgets('muestra el motivo cuando el backend rechaza el depósito', (tester) async {
    server.on('POST', '/v1/accounts/deposit-pse',
        const FakeResponse({'code': 'AMOUNT_TOO_LOW', 'message': 'El monto mínimo es \$10.000'}, status: 422));
    final router = await openDeposit(tester);

    await tester.enterText(find.byType(TextField), '5000');
    await tester.pumpAndSettle();
    await tapAndSettle(tester, find.text('DEPÓSITO'));

    expect(find.text('El monto mínimo es \$10.000'), findsOneWidget);
    expect(currentPath(router), '/screen');
  });

  group('PSE', () {
    testWidgets('exige elegir el banco y lo muestra una vez elegido', (tester) async {
      final router = await openDeposit(tester, method: 'PSE');
      await tapAndSettle(tester, find.text('\$100.000'));

      await tapAndSettle(tester, find.text('DEPÓSITO'));
      expect(find.text('Selecciona tu banco para continuar'), findsOneWidget);
      expect(depositRequests(), isEmpty);

      await tapAndSettle(tester, find.text('Selecciona tu banco'));
      expect(server.requests.lastWhere((r) => r.path == '/v1/banks').queryParameters, {'type': 'PSE'});
      await tapAndSettle(tester, find.text('Banco Davivienda'));
      expect(find.text('Banco Davivienda'), findsOneWidget);

      await tapAndSettle(tester, find.text('DEPÓSITO'));
      expect(currentPath(router), '/deposit/webview');
    });

    testWidgets('preselecciona el banco del acceso directo', (tester) async {
      await openDeposit(tester, method: 'PSE', bankHint: 'davivienda');

      expect(find.text('Banco Davivienda'), findsOneWidget);
      expect(find.text('Selecciona tu banco'), findsNothing);
    });

    testWidgets('avisa cuando no puede cargar los bancos', (tester) async {
      server.on('GET', '/v1/banks', const FakeResponse({}, status: 500));
      await openDeposit(tester, method: 'PSE');

      await tapAndSettle(tester, find.text('Selecciona tu banco'));

      expect(find.textContaining('No fue posible cargar la lista de bancos'), findsOneWidget);
    });
  });
}
