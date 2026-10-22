import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met/features/auth/presentation/pages/pin_recovery_page.dart';
import 'package:met/features/external_accounts/presentation/pages/payout_page.dart';
import 'package:met/features/transfers/presentation/pages/transfer_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';
import '../helpers/test_rsa_keys.dart';

/// Pasos con campos de texto en un celular pequeño (360×640) con el teclado abierto: el contenido
/// no debe quedar tapado ni desbordarse.
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
      ..on('GET', '/v1/accounts/recent-recipients', const FakeResponse([]))
      ..on('GET', '/v1/accounts/verify', const FakeResponse({'accountId': 'acc-9', 'ownerName': 'Luis R***'}))
      ..on('GET', '/v1/accounts/external-bank-accounts', const FakeResponse([
        {'id': 'ext-1', 'bankCode': '1007', 'bankName': 'Bancolombia', 'accountType': 'SAVINGS',
          'maskedAccountNumber': '****6789', 'verificationStatus': 'VERIFIED', 'createdAt': '2026-10-20T10:00:00Z'},
      ]))
      ..on('POST', '/v1/auth/pin-recovery/request', const FakeResponse({}));
  });

  Future<void> openKeyboard(WidgetTester tester) async {
    tester.view.physicalSize = const Size(360 * 3, 640 * 3);
    tester.view.viewInsets = const FakeViewPadding(bottom: 280 * 3);
    await tester.pumpAndSettle();
  }

  testWidgets('transferencia: paso del monto', (tester) async {
    await pumpScreen(tester, const TransferPage(), server: server);
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), '4510001234');
    await tester.pumpAndSettle();
    await tester.tap(find.text('Buscar Destinatario'));
    await tester.pumpAndSettle();

    await openKeyboard(tester);

    expect(find.text('Continuar'), findsOneWidget);
  });

  testWidgets('retiro: paso del monto', (tester) async {
    await pumpScreen(tester, const PayoutPage(preselectedAccountId: 'ext-1'), server: server);
    await tester.pumpAndSettle();

    await openKeyboard(tester);

    expect(find.text('Continuar'), findsOneWidget);
  });

  testWidgets('recuperación de PIN: documento y código', (tester) async {
    await pumpScreen(tester, const PinRecoveryPage(), server: server);
    await tester.pumpAndSettle();
    await openKeyboard(tester);
    expect(find.text('Solicitar Código'), findsOneWidget);

    await tester.enterText(find.byType(TextFormField), '1086578123');
    await tester.tap(find.text('Solicitar Código'));
    await tester.pumpAndSettle();
    expect(find.text('Validar Código'), findsOneWidget);
  });
}
