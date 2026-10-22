import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/features/external_accounts/presentation/pages/add_external_bank_account_page.dart';
import 'package:met/features/external_accounts/presentation/pages/external_bank_accounts_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

Map<String, dynamic> _account(String id, String bank, String status, {bool depositSent = false}) => {
      'id': id,
      'bankCode': '1007',
      'bankName': bank,
      'accountType': 'SAVINGS',
      'maskedAccountNumber': '****6789',
      'verificationStatus': status,
      'verificationPending': depositSent,
      'createdAt': '2026-10-20T10:00:00Z',
    };

/// Cuentas bancarias propias para retirar: listado, verificación con microdepósito y registro.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter()
      ..on('GET', '/v1/accounts/external-bank-accounts', FakeResponse([
        _account('ext-1', 'Bancolombia', 'VERIFIED'),
        _account('ext-2', 'Davivienda', 'PENDING'),
        _account('ext-3', 'BBVA', 'PENDING', depositSent: true),
        _account('ext-4', 'Banco de Bogotá', 'REJECTED'),
      ]))
      ..on('POST', '/v1/accounts/external-bank-accounts/ext-2/verify/resend', const FakeResponse({}))
      ..on('POST', '/v1/accounts/external-bank-accounts/ext-3/verify', const FakeResponse({}))
      ..on('GET', '/v1/banks', const FakeResponse([
        {'code': '1007', 'name': 'Bancolombia', 'supportsPayout': true},
        {'code': '1051', 'name': 'Davivienda', 'supportsPayout': true},
      ]))
      ..on('POST', '/v1/accounts/external-bank-accounts', const FakeResponse({}));
  });

  Future<void> tapAndSettle(WidgetTester tester, Finder finder) async {
    await tester.ensureVisible(finder);
    await tester.pumpAndSettle();
    await tester.tap(finder);
    await tester.pumpAndSettle();
  }

  int listLoads() => server.requests.where((r) => r.method == 'GET' && r.path == '/v1/accounts/external-bank-accounts').length;

  group('Listado', () {
    Future<GoRouter> openList(WidgetTester tester) async {
      final router = await pumpScreen(tester, const ExternalBankAccountsPage(),
          server: server, destinations: {'/payout', '/accounts/external/add'});
      await tester.pumpAndSettle();
      return router;
    }

    Finder cardOf(String bank) => find.ancestor(of: find.text(bank), matching: find.byType(Card));

    testWidgets('muestra el estado de cada cuenta y lo que se puede hacer con ella', (tester) async {
      await openList(tester);

      expect(find.descendant(of: cardOf('Bancolombia'), matching: find.text('Verificada')), findsOneWidget);
      expect(find.descendant(of: cardOf('Davivienda'), matching: find.text('Enviar verificación')), findsOneWidget);
      expect(find.descendant(of: cardOf('BBVA'), matching: find.text('Confirmar depósito')), findsOneWidget);
      expect(find.descendant(of: cardOf('Banco de Bogotá'), matching: find.text('Rechazada')), findsOneWidget);
      expect(find.descendant(of: cardOf('Banco de Bogotá'), matching: find.text('Retirar a esta cuenta')), findsNothing);
    });

    testWidgets('abre el retiro con la cuenta elegida', (tester) async {
      final router = await openList(tester);

      await tapAndSettle(tester, find.descendant(of: cardOf('Bancolombia'), matching: find.text('Retirar a esta cuenta')));

      expect(currentPath(router), '/payout');
      expect(currentExtra(router), {'accountId': 'ext-1'});
    });

    testWidgets('envía el microdepósito de verificación', (tester) async {
      await openList(tester);

      await tapAndSettle(tester, find.text('Enviar verificación'));

      expect(server.requests.any((r) => r.path.endsWith('/ext-2/verify/resend')), isTrue);
      expect(find.text('Depósito de verificación enviado. Revisa tu extracto bancario.'), findsOneWidget);
    });

    testWidgets('confirma la cuenta con el monto del microdepósito', (tester) async {
      await openList(tester);

      await tapAndSettle(tester, find.text('Confirmar depósito'));
      await tester.enterText(find.widgetWithText(TextField, 'Monto recibido (\$)'), '2731');
      await tapAndSettle(tester, find.text('Confirmar'));

      expect(server.requests.singleWhere((r) => r.path.endsWith('/ext-3/verify')).json, {'amount': 2731});
      expect(find.text('¡Cuenta verificada!'), findsOneWidget);
    });

    testWidgets('explica si el monto no coincide', (tester) async {
      server.on('POST', '/v1/accounts/external-bank-accounts/ext-3/verify',
          const FakeResponse({'code': 'AMOUNT_MISMATCH', 'message': 'El monto no coincide. Te quedan 2 intentos'}, status: 422));
      await openList(tester);

      await tapAndSettle(tester, find.text('Confirmar depósito'));
      await tester.enterText(find.widgetWithText(TextField, 'Monto recibido (\$)'), '1000');
      await tapAndSettle(tester, find.text('Confirmar'));

      expect(find.text('El monto no coincide. Te quedan 2 intentos'), findsOneWidget);
    });

    testWidgets('invita a registrar la primera cuenta y recarga al volver', (tester) async {
      server.on('GET', '/v1/accounts/external-bank-accounts', const FakeResponse([]));
      final router = await openList(tester);
      expect(find.text('Aún no tienes cuentas bancarias registradas'), findsOneWidget);

      await tapAndSettle(tester, find.text('Agregar cuenta'));
      expect(currentPath(router), '/accounts/external/add');
      final loadsBefore = listLoads();
      router.pop();
      await tester.pumpAndSettle();

      expect(listLoads(), loadsBefore + 1);
    });

    testWidgets('si no carga, explica el error sin texto técnico y permite reintentar', (tester) async {
      server.on('GET', '/v1/accounts/external-bank-accounts', const FakeResponse({}, status: 503));
      await openList(tester);

      expect(find.text('Error en el servidor. Intenta más tarde.'), findsOneWidget);
      expect(find.textContaining('DioException'), findsNothing);

      server.on('GET', '/v1/accounts/external-bank-accounts', const FakeResponse([]));
      await tapAndSettle(tester, find.text('Reintentar'));
      expect(find.text('Aún no tienes cuentas bancarias registradas'), findsOneWidget);
    });
  });

  group('Registro', () {
    Future<GoRouter> openForm(WidgetTester tester) async {
      final router = await pumpScreen(tester, const AddExternalBankAccountPage(), server: server);
      await tester.pumpAndSettle();
      return router;
    }

    Future<void> choose(WidgetTester tester, String field, String option) async {
      await tapAndSettle(tester, find.widgetWithText(DropdownButtonFormField<String>, field));
      await tapAndSettle(tester, find.text(option).last);
    }

    testWidgets('registra la cuenta y vuelve al listado', (tester) async {
      final router = await openForm(tester);
      expect(server.requests.singleWhere((r) => r.path == '/v1/banks').queryParameters, {'type': 'PAYOUT'});

      await choose(tester, 'Banco', 'Davivienda');
      await choose(tester, 'Tipo de cuenta', 'Corriente');
      await tester.enterText(find.widgetWithText(TextFormField, 'Número de cuenta'), '00123456789');
      await tapAndSettle(tester, find.text('Registrar cuenta'));

      expect(server.requests.singleWhere((r) => r.method == 'POST').json,
          {'bankCode': '1051', 'accountType': 'CHECKING', 'accountNumber': '00123456789'});
      expect(currentPath(router), previousScreen);
    });

    testWidgets('pide el banco y un número de cuenta válido', (tester) async {
      await openForm(tester);

      await tester.enterText(find.widgetWithText(TextFormField, 'Número de cuenta'), '12345');
      await tapAndSettle(tester, find.text('Registrar cuenta'));

      expect(find.text('Debe tener entre 6 y 20 dígitos'), findsOneWidget);
      expect(find.text('Selecciona un banco'), findsOneWidget);
      expect(server.requests.where((r) => r.method == 'POST'), isEmpty);
    });

    testWidgets('solo acepta dígitos en el número de cuenta', (tester) async {
      await openForm(tester);

      await tester.enterText(find.widgetWithText(TextFormField, 'Número de cuenta'), '12-34 56ab78');

      expect(find.text('12345678'), findsOneWidget);
    });

    testWidgets('muestra el motivo si el backend rechaza la cuenta', (tester) async {
      server.on('POST', '/v1/accounts/external-bank-accounts',
          const FakeResponse({'code': 'DUPLICATE_ACCOUNT', 'message': 'Ya registraste esta cuenta'}, status: 409));
      final router = await openForm(tester);

      await choose(tester, 'Banco', 'Bancolombia');
      await tester.enterText(find.widgetWithText(TextFormField, 'Número de cuenta'), '00123456789');
      await tapAndSettle(tester, find.text('Registrar cuenta'));

      expect(find.text('Ya registraste esta cuenta'), findsOneWidget);
      expect(currentPath(router), '/screen');
    });

    testWidgets('avisa si no puede cargar los bancos', (tester) async {
      server.on('GET', '/v1/banks', const FakeResponse({}, status: 500));
      await openForm(tester);

      expect(find.text('No fue posible cargar la lista de bancos'), findsOneWidget);
    });
  });
}
