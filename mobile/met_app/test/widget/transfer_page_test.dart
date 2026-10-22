import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/features/transfers/presentation/pages/transfer_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';
import '../helpers/test_rsa_keys.dart';

/// La transferencia entre socios paso a paso (destinatario, monto, PIN, código y confirmación)
/// contra un servidor falso.
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
      ..on('GET', '/v1/accounts/recent-recipients', const FakeResponse([
        {'accountId': 'acc-9', 'accountNumber': '4510001234', 'ownerName': 'luis R***'},
      ]))
      ..on('GET', '/v1/accounts/verify', const FakeResponse({'accountId': 'acc-9', 'ownerName': 'Luis R***'}))
      ..on('POST', '/v1/accounts/transactions/transfer', const FakeResponse({}))
      ..on('POST', '/v1/accounts/transactions/transfer/otp/request', const FakeResponse({}));
  });

  Future<GoRouter> openTransfer(WidgetTester tester) async {
    final router = await pumpScreen(tester, const TransferPage(), server: server, destinations: {'/'});
    await tester.pumpAndSettle();
    return router;
  }

  Future<void> tapAndSettle(WidgetTester tester, Finder finder) async {
    await tester.tap(finder);
    await tester.pumpAndSettle();
  }

  Future<void> typeInto(WidgetTester tester, String label, String text) async {
    await tester.enterText(find.widgetWithText(TextField, label), text);
    await tester.pumpAndSettle();
  }

  /// El PIN y el código se escriben en el único campo de su paso.
  Future<void> typeInStep(WidgetTester tester, String text) async {
    await tester.enterText(find.byType(TextField), text);
    await tester.pumpAndSettle();
  }

  Future<void> goToAmountStep(WidgetTester tester) async {
    await typeInto(tester, 'Número de cuenta o celular', '4510001234');
    await tapAndSettle(tester, find.text('Buscar Destinatario'));
  }

  Future<void> goToPinStep(WidgetTester tester, {String amount = '150000'}) async {
    await goToAmountStep(tester);
    await typeInto(tester, 'Monto a transferir (\$)', amount);
    await typeInto(tester, 'Concepto o mensaje (Opcional)', 'Arriendo');
    await tapAndSettle(tester, find.text('Continuar'));
  }

  RecordedRequest transferRequest() => server.requests.lastWhere((r) => r.path == '/v1/accounts/transactions/transfer');

  FilledButton button(WidgetTester tester, String text) =>
      tester.widget<FilledButton>(find.ancestor(of: find.text(text), matching: find.byType(FilledButton)));

  testWidgets('elige un destinatario reciente y lo verifica', (tester) async {
    await openTransfer(tester);
    expect(find.text('luis R***'), findsOneWidget);

    await tapAndSettle(tester, find.text('luis R***'));
    expect(find.text('Recientes'), findsNothing, reason: 'con un número escrito ya no se muestran los recientes');
    await tapAndSettle(tester, find.text('Buscar Destinatario'));

    expect(server.requests.lastWhere((r) => r.path == '/v1/accounts/verify').queryParameters,
        {'identifier': '4510001234'});
    expect(find.text('Enviando a:'), findsOneWidget);
    expect(find.text('Luis R***'), findsOneWidget);
  });

  testWidgets('avisa cuando aún no tiene destinatarios recientes', (tester) async {
    server.on('GET', '/v1/accounts/recent-recipients', const FakeResponse([]));
    await openTransfer(tester);

    expect(find.text('Aún no tienes destinatarios recientes'), findsOneWidget);
  });

  testWidgets('pide el número antes de buscar y lo borra con la X', (tester) async {
    await openTransfer(tester);

    await tapAndSettle(tester, find.text('Buscar Destinatario'));
    expect(find.text('Ingresa un número de cuenta o celular'), findsOneWidget);
    expect(server.requests.where((r) => r.path == '/v1/accounts/verify'), isEmpty);

    await typeInto(tester, 'Número de cuenta o celular', '999');
    await tapAndSettle(tester, find.byIcon(Icons.clear));
    expect(find.text('Recientes'), findsOneWidget);
  });

  testWidgets('muestra el motivo cuando el destinatario no existe', (tester) async {
    server.on('GET', '/v1/accounts/verify',
        const FakeResponse({'code': 'NOT_FOUND', 'message': 'No existe una cuenta con ese número'}, status: 404));
    await openTransfer(tester);

    await goToAmountStep(tester);

    expect(find.text('No existe una cuenta con ese número'), findsOneWidget);
    expect(find.text('Buscar Destinatario'), findsOneWidget);
  });

  testWidgets('un nombre largo del destinatario no desborda la tarjeta', (tester) async {
    server.on('GET', '/v1/accounts/verify',
        const FakeResponse({'accountId': 'acc-9', 'ownerName': 'María Fernanda Rodríguez G***'}));
    await openTransfer(tester);

    await goToAmountStep(tester);

    expect(find.text('María Fernanda Rodríguez G***'), findsOneWidget);
  });

  testWidgets('si no puede cargar el saldo no rompe la pantalla ni deja continuar', (tester) async {
    server.on('GET', '/v1/accounts/me', const FakeResponse({'message': 'Servicio no disponible'}, status: 503));
    await openTransfer(tester);

    await goToAmountStep(tester);
    await typeInto(tester, 'Monto a transferir (\$)', '150000');

    expect(find.text('Enviando a:'), findsOneWidget);
    expect(button(tester, 'Continuar').onPressed, isNull);
  });

  testWidgets('solo deja transferir hasta los intereses disponibles', (tester) async {
    await openTransfer(tester);
    await goToAmountStep(tester);
    expect(button(tester, 'Continuar').onPressed, isNull, reason: 'sin monto no se puede continuar');

    await typeInto(tester, 'Monto a transferir (\$)', '250000');
    expect(find.text('El monto supera tus intereses disponibles (\$200000.00)'), findsOneWidget);
    expect(button(tester, 'Continuar').onPressed, isNull);

    await typeInto(tester, 'Monto a transferir (\$)', '150000');
    expect(find.textContaining('supera tus intereses'), findsNothing);
    expect(button(tester, 'Continuar').onPressed, isNotNull);
  });

  testWidgets('confirma con el PIN, pide el código del correo y completa la transferencia', (tester) async {
    // El backend siempre exige el código: el primer envío (solo PIN) responde OTP_REQUIRED.
    server.on('POST', '/v1/accounts/transactions/transfer', const FakeResponse(
        {'code': 'OTP_REQUIRED', 'message': 'Se requiere código de seguridad para transferir'}, status: 422));
    final router = await openTransfer(tester);
    await goToPinStep(tester);
    expect(find.text('\$150000.00'), findsOneWidget);
    expect(find.text('a Luis R***'), findsOneWidget);

    await typeInStep(tester, '4821');
    await tapAndSettle(tester, find.text('Confirmar y Enviar'));

    final first = transferRequest().json;
    expect(first['destinationAccountId'], 'acc-9');
    expect(first['amount'], 150000.0);
    expect(first['concept'], 'Arriendo');
    expect(first.containsKey('otp'), isFalse);
    expect(decryptWithTestPrivateKey(first['pin'] as String), '4821');
    expect(find.text('Código de Seguridad (OTP)'), findsOneWidget);
    expect(server.requests.where((r) => r.path.endsWith('/otp/request')), hasLength(1));

    await typeInStep(tester, '48291');
    expect(button(tester, 'Validar y Transferir').onPressed, isNull, reason: 'el código tiene 6 dígitos');

    server.on('POST', '/v1/accounts/transactions/transfer', const FakeResponse({}));
    await typeInStep(tester, '482913');
    await tapAndSettle(tester, find.text('Validar y Transferir'));

    final second = transferRequest().json;
    expect(second['otp'], '482913');
    expect(second['idempotencyKey'], first['idempotencyKey'], reason: 'es la misma transferencia, reintentada con el código');
    expect(decryptWithTestPrivateKey(second['pin'] as String), '4821');
    expect(find.text('¡Transferencia Exitosa!'), findsOneWidget);
    expect(find.text('Se han enviado \$150000.00 a Luis R***'), findsOneWidget);

    await tapAndSettle(tester, find.text('Volver al Inicio'));
    expect(currentPath(router), '/');
  });

  testWidgets('reenvía el código cuando el socio lo pide', (tester) async {
    server.on('POST', '/v1/accounts/transactions/transfer', const FakeResponse(
        {'code': 'OTP_REQUIRED', 'message': 'Se requiere código de seguridad para transferir'}, status: 422));
    await openTransfer(tester);
    await goToPinStep(tester);
    await typeInStep(tester, '4821');
    await tapAndSettle(tester, find.text('Confirmar y Enviar'));

    await tapAndSettle(tester, find.text('Reenviar código'));

    expect(server.requests.where((r) => r.path.endsWith('/otp/request')), hasLength(2));
  });

  testWidgets('se queda en el PIN y explica el rechazo', (tester) async {
    server.on('POST', '/v1/accounts/transactions/transfer',
        const FakeResponse({'code': 'INVALID_PIN', 'message': 'El PIN es incorrecto'}, status: 422));
    await openTransfer(tester);
    await goToPinStep(tester);

    await typeInStep(tester, '0000');
    await tapAndSettle(tester, find.text('Confirmar y Enviar'));

    expect(find.text('El PIN es incorrecto'), findsOneWidget);
    expect(find.text('Confirmar y Enviar'), findsOneWidget);
  });

  testWidgets('la flecha atrás vuelve al paso anterior', (tester) async {
    await openTransfer(tester);
    await goToPinStep(tester);

    await tapAndSettle(tester, find.byIcon(Icons.arrow_back));
    expect(find.text('Enviando a:'), findsOneWidget);

    await tapAndSettle(tester, find.byIcon(Icons.arrow_back));
    expect(find.text('¿A quién le vas a enviar?'), findsOneWidget);
  });
}
