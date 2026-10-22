import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/features/external_accounts/presentation/pages/payout_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';
import '../helpers/test_rsa_keys.dart';

Map<String, dynamic> _account(String id, {String status = 'VERIFIED', String bank = 'Bancolombia'}) => {
      'id': id,
      'bankCode': '1007',
      'bankName': bank,
      'accountType': 'SAVINGS',
      'maskedAccountNumber': '****6789',
      'verificationStatus': status,
      'verificationPending': status == 'PENDING',
      'createdAt': '2026-10-20T10:00:00Z',
    };

/// El retiro de ganancias a una cuenta bancaria propia, paso a paso, contra un servidor falso.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter()
      ..on('GET', '/v1/auth/public-key', const FakeResponse({'publicKey': testRsaPublicKeyBase64}))
      ..on('GET', '/v1/accounts/me', const FakeResponse({
        'id': 'acc-1', 'accountNumber': '4510009283', 'principalBalance': 2450000, 'interestBalance': 200000,
        'status': 'ACTIVE',
      }))
      ..on('GET', '/v1/accounts/external-bank-accounts', FakeResponse([
        _account('ext-1'),
        _account('ext-2', status: 'REJECTED', bank: 'Davivienda'),
      ]))
      ..on('POST', '/v1/accounts/transactions/transfer/otp/request', const FakeResponse({}))
      ..on('POST', '/v1/accounts/transactions/payout', const FakeResponse(
          {'code': 'OTP_REQUIRED', 'message': 'Se requiere código de seguridad para retirar'}, status: 422));
  });

  Future<GoRouter> openPayout(WidgetTester tester, {String? preselectedAccountId}) async {
    final router = await pumpScreen(tester, PayoutPage(preselectedAccountId: preselectedAccountId),
        server: server, destinations: {'/', '/accounts/external/add'});
    await tester.pumpAndSettle();
    return router;
  }

  Future<void> tapAndSettle(WidgetTester tester, Finder finder) async {
    await tester.tap(finder);
    await tester.pumpAndSettle();
  }

  Future<void> typeInto(WidgetTester tester, Finder field, String text) async {
    await tester.enterText(field, text);
    await tester.pumpAndSettle();
  }

  Future<void> goToPinStep(WidgetTester tester) async {
    await tapAndSettle(tester, find.text('Bancolombia'));
    await typeInto(tester, find.widgetWithText(TextField, 'Monto a retirar (\$)'), '150000');
    await typeInto(tester, find.widgetWithText(TextField, 'Concepto (Opcional)'), 'Ganancias');
    await tapAndSettle(tester, find.text('Continuar'));
  }

  RecordedRequest payoutRequest() => server.requests.lastWhere((r) => r.path == '/v1/accounts/transactions/payout');

  FilledButton button(WidgetTester tester, String text) =>
      tester.widget<FilledButton>(find.ancestor(of: find.text(text), matching: find.byType(FilledButton)));

  testWidgets('solo ofrece las cuentas que se pueden usar', (tester) async {
    await openPayout(tester);

    expect(find.text('Bancolombia'), findsOneWidget);
    expect(find.text('Ahorros · ****6789'), findsOneWidget);
    expect(find.text('Davivienda'), findsNothing, reason: 'una cuenta rechazada no recibe retiros');
  });

  testWidgets('sin cuentas disponibles invita a registrar una y recarga al volver', (tester) async {
    server.on('GET', '/v1/accounts/external-bank-accounts', FakeResponse([_account('ext-2', status: 'REJECTED')]));
    final router = await openPayout(tester);

    expect(find.text('No tienes cuentas bancarias disponibles'), findsOneWidget);
    await tapAndSettle(tester, find.text('Registrar una cuenta'));
    expect(currentPath(router), '/accounts/external/add');

    server.on('GET', '/v1/accounts/external-bank-accounts', FakeResponse([_account('ext-3', status: 'PENDING')]));
    router.pop();
    await tester.pumpAndSettle();

    expect(find.text('Bancolombia'), findsOneWidget);
  });

  testWidgets('salta al monto cuando llega con una cuenta preseleccionada', (tester) async {
    await openPayout(tester, preselectedAccountId: 'ext-1');

    expect(find.text('Retirando a:'), findsOneWidget);
    expect(find.text('Bancolombia · ****6789'), findsOneWidget);
    expect(find.text('Ganancias disponibles: \$200000.00'), findsOneWidget);
  });

  testWidgets('si no puede cargar el saldo no rompe la pantalla ni deja continuar', (tester) async {
    server.on('GET', '/v1/accounts/me', const FakeResponse({'message': 'Servicio no disponible'}, status: 503));
    await openPayout(tester);
    await tapAndSettle(tester, find.text('Bancolombia'));

    await typeInto(tester, find.widgetWithText(TextField, 'Monto a retirar (\$)'), '150000');

    expect(find.text('Retirando a:'), findsOneWidget);
    expect(button(tester, 'Continuar').onPressed, isNull);
  });

  testWidgets('solo deja retirar hasta las ganancias disponibles', (tester) async {
    await openPayout(tester);
    await tapAndSettle(tester, find.text('Bancolombia'));

    await typeInto(tester, find.widgetWithText(TextField, 'Monto a retirar (\$)'), '250000');
    expect(find.text('El monto supera tus ganancias disponibles (\$200000.00)'), findsOneWidget);
    expect(button(tester, 'Continuar').onPressed, isNull);

    await typeInto(tester, find.widgetWithText(TextField, 'Monto a retirar (\$)'), '150000');
    expect(button(tester, 'Continuar').onPressed, isNotNull);
  });

  testWidgets('confirma con el PIN, pide el código y deja el retiro en proceso', (tester) async {
    final router = await openPayout(tester);
    await goToPinStep(tester);
    expect(find.text('\$150000.00'), findsOneWidget);
    expect(find.text('a Bancolombia'), findsOneWidget);

    await typeInto(tester, find.byType(TextField), '4821');
    await tapAndSettle(tester, find.text('Confirmar y Retirar'));

    final first = payoutRequest().json;
    expect(first['externalBankAccountId'], 'ext-1');
    expect(first['amount'], 150000.0);
    expect(first['concept'], 'Ganancias');
    expect(first.containsKey('otp'), isFalse);
    expect(decryptWithTestPrivateKey(first['pin'] as String), '4821');
    expect(find.text('Código de Seguridad (OTP)'), findsOneWidget);

    await typeInto(tester, find.byType(TextField), '48291');
    expect(button(tester, 'Validar y Retirar').onPressed, isNull);

    server.on('POST', '/v1/accounts/transactions/payout', const FakeResponse({'message': 'Retiro iniciado con éxito'}));
    await typeInto(tester, find.byType(TextField), '482913');
    await tapAndSettle(tester, find.text('Validar y Retirar'));

    final second = payoutRequest().json;
    expect(second['otp'], '482913');
    expect(second['idempotencyKey'], first['idempotencyKey']);
    expect(find.text('¡Retiro Iniciado!'), findsOneWidget);
    expect(find.textContaining('Tu retiro de \$150000.00 está en proceso hacia Bancolombia'), findsOneWidget);

    await tapAndSettle(tester, find.text('Volver al Inicio'));
    expect(currentPath(router), '/');
  });

  testWidgets('reenvía el código y muestra el rechazo del retiro', (tester) async {
    await openPayout(tester);
    await goToPinStep(tester);
    await typeInto(tester, find.byType(TextField), '4821');
    await tapAndSettle(tester, find.text('Confirmar y Retirar'));

    await tapAndSettle(tester, find.text('Reenviar código'));
    expect(server.requests.where((r) => r.path.endsWith('/otp/request')), hasLength(2));

    server.on('POST', '/v1/accounts/transactions/payout',
        const FakeResponse({'code': 'INVALID_OTP', 'message': 'El código de seguridad es incorrecto o expiró'}, status: 422));
    await typeInto(tester, find.byType(TextField), '000000');
    await tapAndSettle(tester, find.text('Validar y Retirar'));

    expect(find.text('El código de seguridad es incorrecto o expiró'), findsOneWidget);
    expect(find.text('Validar y Retirar'), findsOneWidget);
  });

  testWidgets('la flecha atrás vuelve al paso anterior', (tester) async {
    await openPayout(tester);
    await goToPinStep(tester);

    await tapAndSettle(tester, find.byIcon(Icons.arrow_back));
    expect(find.text('Retirando a:'), findsOneWidget);
  });
}
