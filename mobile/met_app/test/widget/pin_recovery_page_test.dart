import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/features/auth/presentation/pages/pin_recovery_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';
import '../helpers/test_rsa_keys.dart';

/// Recuperación del PIN con el código enviado al correo, contra un servidor falso.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter()
      ..on('GET', '/v1/auth/public-key', const FakeResponse({'publicKey': testRsaPublicKeyBase64}))
      ..on('POST', '/v1/auth/pin-recovery/request', const FakeResponse({}))
      ..on('POST', '/v1/auth/pin-recovery/reset', const FakeResponse({}));
  });

  Future<GoRouter> openRecovery(WidgetTester tester) async {
    final router = await pumpScreen(tester, const PinRecoveryPage(), server: server, destinations: {'/login'});
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

  Future<void> requestCode(WidgetTester tester, {String document = '1086578123'}) async {
    await typeInto(tester, find.widgetWithText(TextFormField, 'Número de documento'), document);
    await tapAndSettle(tester, find.text('Solicitar Código'));
  }

  Future<void> enterCode(WidgetTester tester, String code) async {
    await typeInto(tester, find.byType(TextFormField), code);
    await tapAndSettle(tester, find.text('Validar Código'));
  }

  Future<void> choosePin(WidgetTester tester, String pin, {String? confirmation}) async {
    await typeInto(tester, find.widgetWithText(TextFormField, 'Nuevo PIN (4 dígitos)'), pin);
    await typeInto(tester, find.widgetWithText(TextFormField, 'Confirmar Nuevo PIN'), confirmation ?? pin);
    await tapAndSettle(tester, find.text('Actualizar PIN'));
  }

  RecordedRequest lastTo(String path) => server.requests.lastWhere((r) => r.path == path);

  testWidgets('cambia el PIN con el código del correo y vuelve al ingreso', (tester) async {
    final router = await openRecovery(tester);

    await tapAndSettle(tester, find.text('CC'));
    await tapAndSettle(tester, find.text('CE').last);
    await requestCode(tester, document: '987654');
    expect(lastTo('/v1/auth/pin-recovery/request').json, {'documentType': 'CE', 'documentNumber': '987654'});
    expect(find.text('Revisa tu correo'), findsOneWidget);

    await enterCode(tester, '482913');
    expect(find.text('Crea tu nuevo PIN'), findsOneWidget);
    await choosePin(tester, '9375');

    final sent = lastTo('/v1/auth/pin-recovery/reset').json;
    expect(sent['documentType'], 'CE');
    expect(sent['documentNumber'], '987654');
    expect(sent['otpCode'], '482913');
    expect(decryptWithTestPrivateKey(sent['newPin'] as String), '9375');
    expect(currentPath(router), '/login');
  });

  testWidgets('no avanza sin documento ni con un código incompleto', (tester) async {
    await openRecovery(tester);

    await tapAndSettle(tester, find.text('Solicitar Código'));
    expect(server.requests, isEmpty);

    await requestCode(tester);
    await enterCode(tester, '4829');
    expect(find.text('Revisa tu correo'), findsOneWidget);
  });

  testWidgets('no envía el PIN si la confirmación no coincide', (tester) async {
    await openRecovery(tester);
    await requestCode(tester);
    await enterCode(tester, '482913');

    await choosePin(tester, '9375', confirmation: '9376');

    expect(find.text('Los PIN no coinciden'), findsOneWidget);
    expect(server.requests.where((r) => r.path == '/v1/auth/pin-recovery/reset'), isEmpty);
  });

  testWidgets('muestra el motivo si el documento no existe', (tester) async {
    server.on('POST', '/v1/auth/pin-recovery/request',
        const FakeResponse({'code': 'NOT_FOUND', 'message': 'No encontramos ese documento'}, status: 404));
    await openRecovery(tester);

    await requestCode(tester);

    expect(find.text('No encontramos ese documento'), findsOneWidget);
    expect(find.text('Solicitar Código'), findsOneWidget);
  });

  testWidgets('si el código es incorrecto deja volver a escribirlo', (tester) async {
    server.on('POST', '/v1/auth/pin-recovery/reset',
        const FakeResponse({'code': 'INVALID_OTP', 'message': 'El código es incorrecto o expiró'}, status: 422));
    final router = await openRecovery(tester);
    await requestCode(tester);
    await enterCode(tester, '000000');

    await choosePin(tester, '9375');
    expect(find.text('El código es incorrecto o expiró'), findsOneWidget);
    expect(currentPath(router), '/screen');

    await tapAndSettle(tester, find.byIcon(Icons.arrow_back));
    expect(find.text('Revisa tu correo'), findsOneWidget);
  });

  testWidgets('la flecha atrás en el primer paso cierra la pantalla', (tester) async {
    final router = await openRecovery(tester);

    await tapAndSettle(tester, find.byIcon(Icons.arrow_back));

    expect(currentPath(router), previousScreen);
  });

  testWidgets('al volver a abrirla empieza desde el documento', (tester) async {
    final router = await openRecovery(tester);
    await requestCode(tester);
    await enterCode(tester, '482913');
    router.pop();
    await tester.pumpAndSettle();

    router.push('/screen');
    await tester.pumpAndSettle();

    expect(find.text('¿Olvidaste tu PIN?'), findsOneWidget,
        reason: 'no debe retomar el paso del PIN nuevo con el documento y el código de antes');
  });
}
