import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/features/transfers/presentation/pages/deposit_waiting_page.dart';
import 'package:met/features/transfers/presentation/providers/deposit_provider.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

FakeResponse _account(double principal) => FakeResponse({
      'id': 'acc-1', 'accountNumber': '4510009283', 'principalBalance': principal, 'interestBalance': 0,
      'status': 'ACTIVE',
    });

/// Espera del pago en la pasarela: solo se da por hecho cuando el saldo real sube (lo acredita el
/// webhook de Wompi en el backend), contra un servidor falso.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter()
      ..on('GET', '/v1/accounts/me', _account(2450000))
      ..on('POST', '/v1/accounts/deposit-pse', const FakeResponse({'paymentUrl': 'https://checkout.wompi.co/l/abc123'}));
  });

  /// Abre la espera como lo hace la app: tras crear el enlace de pago, con el saldo previo guardado.
  Future<GoRouter> openWaiting(WidgetTester tester) async {
    final router = await pumpScreen(tester, const DepositWaitingPage(method: 'Nequi', amount: 50000),
        server: server, destinations: {'/home'});
    // Sin await: las peticiones avanzan con los cuadros del reloj falso de la prueba.
    ProviderScope.containerOf(tester.element(find.byType(DepositWaitingPage))).read(depositProvider.notifier)
      ..setAmount(50000)
      ..submitDeposit('Nequi');
    for (var i = 0; i < 3; i++) {
      await tester.pump(const Duration(milliseconds: 100));
    }
    expect(server.requests.map((r) => r.path), contains('/v1/accounts/deposit-pse'));
    return router;
  }

  testWidgets('muestra el monto y descuenta el tiempo para pagar', (tester) async {
    await openWaiting(tester);

    expect(find.text('Finaliza el pago en tu Nequi'), findsOneWidget);
    expect(find.textContaining('+57300'), findsNothing, reason: 'no debe mostrar un celular inventado');
    expect(find.text('05:00'), findsOneWidget);

    await tester.pump(const Duration(seconds: 1));
    expect(find.text('04:59'), findsOneWidget);
  });

  testWidgets('confirma el depósito cuando el saldo real sube y vuelve al inicio', (tester) async {
    final router = await openWaiting(tester);

    await tester.pump(const Duration(seconds: 3));
    await tester.pump();
    expect(find.text('¡Pago Exitoso!'), findsNothing, reason: 'el saldo aún no cambió');

    server.on('GET', '/v1/accounts/me', _account(2500000));
    await tester.pump(const Duration(seconds: 3));
    await tester.pumpAndSettle();
    expect(find.text('¡Pago Exitoso!'), findsOneWidget);

    await tester.tap(find.text('Aceptar'));
    await tester.pumpAndSettle();
    expect(currentPath(router), '/home');
  });

  testWidgets('a los cinco minutos sin acreditación lo explica y ofrece volver', (tester) async {
    final router = await openWaiting(tester);

    await tester.pump(const Duration(seconds: 301));
    await tester.pumpAndSettle();

    expect(find.textContaining('No hemos recibido la confirmación todavía'), findsOneWidget);
    expect(find.text('¡Pago Exitoso!'), findsNothing);
    final polls = server.requests.where((r) => r.path == '/v1/accounts/me').length;

    await tester.pump(const Duration(seconds: 30));
    expect(server.requests.where((r) => r.path == '/v1/accounts/me').length, polls, reason: 'deja de consultar el saldo');

    await tester.tap(find.text('Volver al inicio'));
    await tester.pumpAndSettle();
    expect(currentPath(router), '/home');
  });

  testWidgets('la X vuelve al inicio sin esperar', (tester) async {
    final router = await openWaiting(tester);

    await tester.tap(find.byTooltip('Volver al inicio'));
    await tester.pumpAndSettle();

    expect(currentPath(router), '/home');
  });
}
