import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/features/auth/presentation/pages/email_verification_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

/// Verificación del correo con el código enviado al registrarse, contra un servidor falso.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter()
      ..on('POST', '/v1/auth/verify-email', const FakeResponse({}))
      ..on('POST', '/v1/auth/resend-email-otp', const FakeResponse({}));
  });

  Future<GoRouter> openVerification(WidgetTester tester) async {
    final router = await pumpScreen(tester, const EmailVerificationPage(documentType: 'CE', documentNumber: '987654'),
        server: server, destinations: {'/login'});
    await tester.pump();
    return router;
  }

  Future<void> verify(WidgetTester tester, String code) async {
    await tester.enterText(find.byType(TextField), code);
    await tester.pump();
    await tester.tap(find.text('Verificar'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));
  }

  TextButton resendButton(WidgetTester tester) =>
      tester.widget<TextButton>(find.ancestor(of: find.textContaining('Reenviar código'), matching: find.byWidgetPredicate((w) => w is TextButton)));

  testWidgets('verifica el correo con el documento y vuelve al ingreso', (tester) async {
    final router = await openVerification(tester);

    await verify(tester, '482913');

    expect(server.last.json, {'documentType': 'CE', 'documentNumber': '987654', 'otpCode': '482913'});
    expect(currentPath(router), '/login');
  });

  testWidgets('no envía un código incompleto', (tester) async {
    await openVerification(tester);

    await verify(tester, '4829');

    expect(server.requests, isEmpty);
  });

  testWidgets('muestra el motivo si el código es incorrecto y lo borra al escribir de nuevo', (tester) async {
    server.on('POST', '/v1/auth/verify-email', const FakeResponse({'code': 'INVALID_OTP', 'message': 'El código expiró'}, status: 422));
    await openVerification(tester);

    await verify(tester, '000000');
    expect(find.text('El código expiró'), findsOneWidget);

    await tester.enterText(find.byType(TextField), '4');
    await tester.pump();
    expect(find.text('El código expiró'), findsNothing);
  });

  testWidgets('el reenvío espera 60 segundos y luego reinicia la espera', (tester) async {
    await openVerification(tester);
    expect(find.text('Reenviar código en 60s'), findsOneWidget);
    expect(resendButton(tester).onPressed, isNull);

    await tester.pump(const Duration(seconds: 60));
    expect(find.text('Reenviar código'), findsOneWidget);

    await tester.tap(find.text('Reenviar código'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));

    expect(server.last.json, {'documentType': 'CE', 'documentNumber': '987654'});
    expect(find.text('Código reenviado. Revisa tu correo.'), findsOneWidget);
    expect(find.textContaining('Reenviar código en'), findsOneWidget);
  });

  testWidgets('explica si no pudo reenviar el código', (tester) async {
    server.on('POST', '/v1/auth/resend-email-otp', const FakeResponse({}, status: 500));
    await openVerification(tester);
    await tester.pump(const Duration(seconds: 60));

    await tester.tap(find.text('Reenviar código'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));

    expect(find.text('No se pudo reenviar el código. Intenta de nuevo.'), findsOneWidget);
  });

  testWidgets('con el teclado abierto en un celular pequeño no se desborda', (tester) async {
    await openVerification(tester);

    tester.view.physicalSize = const Size(360 * 3, 640 * 3);
    tester.view.viewInsets = const FakeViewPadding(bottom: 280 * 3);
    await tester.pump();

    expect(find.text('Verificar'), findsOneWidget);
  });
}
