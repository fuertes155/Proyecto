import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/features/auth/presentation/pages/security_settings_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';
import '../helpers/test_rsa_keys.dart';

Map<String, dynamic> _profile({bool biometricLoginEnabled = false}) => {
      'id': 'user-1',
      'firstName': 'Ana',
      'lastName': 'Pérez',
      'email': 'socio@example.com',
      'phone': '3001234567',
      'status': 'ACTIVE',
      'kycStatus': 'APPROVED',
      'documentType': 'CC',
      'documentNumber': '1086578123',
      'biometricLoginEnabled': biometricLoginEnabled,
    };

/// Cambio de PIN e ingreso con huella desde Perfil > Seguridad, contra un servidor falso.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter()
      ..on('GET', '/v1/auth/public-key', const FakeResponse({'publicKey': testRsaPublicKeyBase64}))
      ..on('GET', '/v1/auth/me', FakeResponse(_profile()))
      ..on('PUT', '/v1/auth/pin', const FakeResponse({}))
      ..on('PUT', '/v1/auth/biometric-login', const FakeResponse({}))
      ..on('DELETE', '/v1/auth/biometric-login', const FakeResponse({}));
  });

  /// El celular tiene una huella registrada y el socio la confirma cuando se le pide.
  void givenFingerprintEnrolled() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('plugins.flutter.io/local_auth'), (call) async {
      return switch (call.method) {
        'getAvailableBiometrics' => <String>['fingerprint'],
        _ => true,
      };
    });
  }

  Future<GoRouter> openSecurity(WidgetTester tester) async {
    final router = await pumpScreen(tester, const SecuritySettingsPage(), server: server);
    await tester.pumpAndSettle();
    return router;
  }

  Future<void> fillPins(WidgetTester tester, {required String current, required String next, String? confirm}) async {
    await tester.enterText(find.widgetWithText(TextFormField, 'PIN actual'), current);
    await tester.enterText(find.widgetWithText(TextFormField, 'Nuevo PIN (4 dígitos)'), next);
    await tester.enterText(find.widgetWithText(TextFormField, 'Confirmar nuevo PIN'), confirm ?? next);
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Actualizar PIN'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Actualizar PIN'));
    await tester.pumpAndSettle();
  }

  group('Cambio de PIN', () {
    testWidgets('envía el PIN actual y el nuevo cifrados y vuelve atrás', (tester) async {
      final router = await openSecurity(tester);

      await fillPins(tester, current: '4821', next: '9375');

      final sent = server.requests.singleWhere((r) => r.path == '/v1/auth/pin').json;
      expect(decryptWithTestPrivateKey(sent['currentPin'] as String), '4821');
      expect(decryptWithTestPrivateKey(sent['newPin'] as String), '9375');
      expect(find.text('PIN actualizado correctamente'), findsOneWidget);
      expect(currentPath(router), previousScreen);
    });

    testWidgets('exige un PIN nuevo de exactamente 4 dígitos, como el teclado del ingreso', (tester) async {
      await openSecurity(tester);

      await fillPins(tester, current: '4821', next: '937512');

      expect(find.text('El PIN debe tener 4 dígitos'), findsOneWidget,
          reason: 'con 6 dígitos el socio ya no podría entrar: el teclado del ingreso solo acepta 4');
      expect(server.requests.where((r) => r.path == '/v1/auth/pin'), isEmpty);
    });

    testWidgets('no envía nada si la confirmación no coincide', (tester) async {
      await openSecurity(tester);

      await fillPins(tester, current: '4821', next: '9375', confirm: '9376');

      expect(find.text('El nuevo PIN no coincide'), findsOneWidget);
      expect(server.requests.where((r) => r.path == '/v1/auth/pin'), isEmpty);
    });

    testWidgets('muestra el motivo del backend si el PIN actual es incorrecto', (tester) async {
      server.on('PUT', '/v1/auth/pin', const FakeResponse({'code': 'INVALID_PIN', 'message': 'El PIN actual es incorrecto'}, status: 422));
      final router = await openSecurity(tester);

      await fillPins(tester, current: '0000', next: '9375');

      expect(find.text('El PIN actual es incorrecto'), findsOneWidget);
      expect(find.textContaining('DioException'), findsNothing);
      expect(currentPath(router), '/screen');
    });
  });

  group('Ingreso con huella', () {
    testWidgets('no aparece si el celular no tiene huella registrada', (tester) async {
      await openSecurity(tester);

      expect(find.text('Entrar con mi huella'), findsNothing);
    });

    testWidgets('se activa confirmando el PIN y guarda la llave en este celular', (tester) async {
      givenFingerprintEnrolled();
      await openSecurity(tester);
      expect(tester.widget<SwitchListTile>(find.byType(SwitchListTile)).value, isFalse);

      await tester.tap(find.text('Entrar con mi huella'));
      await tester.pumpAndSettle();
      await tester.enterText(find.widgetWithText(TextField, 'PIN actual').last, '4821');
      await tester.tap(find.text('Continuar'));
      await tester.pumpAndSettle();

      final sent = server.requests.singleWhere((r) => r.path == '/v1/auth/biometric-login').json;
      expect(decryptWithTestPrivateKey(sent['pin'] as String), '4821');
      expect(await const FlutterSecureStorage().read(key: 'biometric_token'), sent['biometricToken']);
      expect(tester.widget<SwitchListTile>(find.byType(SwitchListTile)).value, isTrue);
      expect(find.text('Listo. La próxima vez puedes entrar con tu huella.'), findsOneWidget);
    });

    testWidgets('se desactiva y olvida la llave de este celular', (tester) async {
      mockNativePlugins(storage: {
        'biometric_token': 'bio-token',
        'biometric_document_type': 'CC',
        'biometric_document_number': '1086578123',
      });
      givenFingerprintEnrolled();
      server.on('GET', '/v1/auth/me', FakeResponse(_profile(biometricLoginEnabled: true)));
      await openSecurity(tester);
      expect(tester.widget<SwitchListTile>(find.byType(SwitchListTile)).value, isTrue);

      await tester.tap(find.text('Entrar con mi huella'));
      await tester.pumpAndSettle();

      expect(server.last.method, 'DELETE');
      expect(await const FlutterSecureStorage().read(key: 'biometric_token'), isNull);
      expect(find.text('Desactivaste el ingreso con huella.'), findsOneWidget);
    });

    testWidgets('avisa si está activa en otro celular y borra una llave que ya no sirve', (tester) async {
      mockNativePlugins(storage: {
        'biometric_token': 'llave-de-otra-persona',
        'biometric_document_type': 'CC',
        'biometric_document_number': '999',
      });
      givenFingerprintEnrolled();
      server.on('GET', '/v1/auth/me', FakeResponse(_profile(biometricLoginEnabled: true)));

      await openSecurity(tester);

      expect(find.textContaining('Está activa en otro celular'), findsOneWidget);
      expect(tester.widget<SwitchListTile>(find.byType(SwitchListTile)).value, isFalse);
      expect(await const FlutterSecureStorage().read(key: 'biometric_token'), isNull);
    });
  });
}
