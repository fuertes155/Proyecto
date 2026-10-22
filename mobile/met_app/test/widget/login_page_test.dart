import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/core/session/session_expired_provider.dart';
import 'package:met/features/auth/presentation/pages/login_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';
import '../helpers/test_rsa_keys.dart';

const _user = {
  'id': 'user-1',
  'firstName': 'Ana',
  'lastName': 'Pérez',
  'email': 'socio@example.com',
  'phone': '3001234567',
  'status': 'ACTIVE',
  'kycStatus': 'APPROVED',
  'emailNotificationsEnabled': true,
  'pushNotificationsEnabled': false,
};

const _tokens = {'userId': 'user-1', 'accessToken': 'acc-1', 'refreshToken': 'ref-1', 'expiresInMs': 1800000};

/// La pantalla de ingreso de punta a punta: teclado del PIN, almacenamiento seguro, cifrado del
/// PIN y navegación, contra un servidor falso.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter()
      ..on('GET', '/v1/auth/public-key', const FakeResponse({'publicKey': testRsaPublicKeyBase64}))
      ..on('POST', '/v1/auth/login', const FakeResponse(_tokens))
      ..on('GET', '/v1/auth/me', const FakeResponse(_user));
  });

  Future<GoRouter> openLogin(WidgetTester tester, {List<Override> overrides = const []}) async {
    final router = await pumpScreen(tester, const LoginPage(),
        server: server,
        overrides: overrides,
        destinations: {'/home', '/biometric-registration', '/register', '/auth/recover-pin', '/verify-email'});
    await tester.pumpAndSettle();
    return router;
  }

  Future<void> tapVisible(WidgetTester tester, Finder finder) async {
    await tester.ensureVisible(finder);
    await tester.tap(finder);
    await tester.pump();
  }

  /// Al enfocar el campo, la pantalla se desplaza animada hasta el cursor y mientras tanto ignora
  /// los toques: se espera a que termine, como haría una persona antes de ir al teclado del PIN.
  Future<void> enterDocument(WidgetTester tester, String document) async {
    await tester.enterText(find.widgetWithText(TextFormField, 'Número de documento'), document);
    await tester.pumpAndSettle();
  }

  /// Teclea como una persona: la biometría conductual bloquea pulsaciones a menos de 50 ms.
  Future<void> typePin(WidgetTester tester, String pin) async {
    for (final digit in pin.split('')) {
      await tapVisible(tester, find.text(digit));
      await tester.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 70)));
    }
  }

  Future<void> login(WidgetTester tester, {String document = '1086578123', String pin = '4821'}) async {
    await enterDocument(tester, document);
    await typePin(tester, pin);
    await tapVisible(tester, find.text('Ingresar'));
    await tester.pumpAndSettle();
  }

  List<RecordedRequest> loginRequests() => server.requests.where((r) => r.path == '/v1/auth/login').toList();

  Future<String?> savedDocument() => const FlutterSecureStorage().read(key: 'saved_document');

  testWidgets('entra con el PIN cifrado y abre el inicio', (tester) async {
    final router = await openLogin(tester);

    await login(tester);

    final sent = loginRequests().single.json;
    expect(sent['documentType'], 'CC');
    expect(sent['documentNumber'], '1086578123');
    expect(sent['deviceId'], isNotEmpty);
    expect(decryptWithTestPrivateKey(sent['pin'] as String), '4821');
    expect(currentPath(router), '/home');
    expect(await savedDocument(), isNull, reason: 'sin "Recordarme" no se guarda la cédula');
  });

  testWidgets('envía cédula de extranjería cuando se elige CE', (tester) async {
    await openLogin(tester);

    await tapVisible(tester, find.text('Extranjería (CE)'));
    await login(tester, document: '987654');

    expect(loginRequests().single.json['documentType'], 'CE');
  });

  testWidgets('lleva al registro biométrico si la identidad aún no se verificó', (tester) async {
    server.on('GET', '/v1/auth/me', FakeResponse({..._user, 'kycStatus': 'PENDING'}));
    final router = await openLogin(tester);

    await login(tester);

    expect(currentPath(router), '/biometric-registration');
  });

  testWidgets('con "Recordarme" guarda la cédula y la próxima vez la muestra enmascarada', (tester) async {
    await openLogin(tester);
    await tapVisible(tester, find.byType(Switch));
    await login(tester);
    expect(await savedDocument(), '1086578123');

    // Segundo ingreso: la pantalla carga la cédula guardada y envía la real, no la enmascarada.
    await tester.pumpWidget(const SizedBox());
    server.requests.clear();
    await openLogin(tester);

    expect(find.text('******8123'), findsOneWidget);
    expect(tester.widget<Switch>(find.byType(Switch)).value, isTrue);
    await typePin(tester, '4821');
    await tapVisible(tester, find.text('Ingresar'));
    await tester.pumpAndSettle();

    expect(loginRequests().single.json['documentNumber'], '1086578123');
  });

  testWidgets('al quitar "Recordarme" borra la cédula guardada', (tester) async {
    mockNativePlugins(storage: {'saved_document': '1086578123'});
    await openLogin(tester);

    await tapVisible(tester, find.byType(Switch));
    await typePin(tester, '4821');
    await tapVisible(tester, find.text('Ingresar'));
    await tester.pumpAndSettle();

    expect(await savedDocument(), isNull);
  });

  testWidgets('no envía nada sin documento o con el PIN incompleto', (tester) async {
    await openLogin(tester);

    await tapVisible(tester, find.text('Ingresar'));
    expect(find.text('Campo requerido'), findsOneWidget);

    await enterDocument(tester, '1086578123');
    await typePin(tester, '482');
    await tapVisible(tester, find.text('Ingresar'));
    await tester.pumpAndSettle();

    expect(loginRequests(), isEmpty);
  });

  testWidgets('la tecla de borrar corrige el último dígito', (tester) async {
    await openLogin(tester);
    await enterDocument(tester, '1086578123');

    await typePin(tester, '4829');
    await tapVisible(tester, find.byIcon(Icons.backspace_outlined));
    await typePin(tester, '1');
    await tapVisible(tester, find.text('Ingresar'));
    await tester.pumpAndSettle();

    expect(decryptWithTestPrivateKey(loginRequests().single.json['pin'] as String), '4821');
  });

  testWidgets('el teclado sigue visible mientras se escribe el PIN', (tester) async {
    await openLogin(tester);
    double opacityOf(String key) => tester
        .widgetList<Opacity>(find.ancestor(of: find.text(key), matching: find.byType(Opacity)))
        .fold(1.0, (opacity, widget) => opacity * widget.opacity);
    expect(opacityOf('5'), 1.0);

    await tapVisible(tester, find.text('4'));
    await tester.pump(const Duration(milliseconds: 16));

    expect(opacityOf('5'), 1.0, reason: 'cada tecla no debe repetir la animación de entrada de la pantalla');
    expect(opacityOf('Bienvenido'), 1.0);
  });

  testWidgets('bloquea el intento cuando el PIN se teclea a velocidad de robot', (tester) async {
    await openLogin(tester);
    await enterDocument(tester, '1086578123');
    await tester.ensureVisible(find.text('Ingresar'));

    for (final digit in ['4', '8', '2', '1']) {
      await tester.tap(find.text(digit));
    }
    await tester.tap(find.text('Ingresar'));
    await tester.pumpAndSettle();

    expect(find.textContaining('comportamiento inusual'), findsOneWidget);
    expect(loginRequests(), isEmpty);
  });

  group('errores del servidor', () {
    for (final (code, message) in [
      ('INVALID_CREDENTIALS', 'El PIN es incorrecto. Te quedan 2 intentos.'),
      ('ACCOUNT_LOCKED', 'Tu cuenta quedó bloqueada 15 minutos.'),
      ('RATE_LIMIT_EXCEEDED', 'Demasiados intentos, espera un momento.'),
      (null, 'Servicio en mantenimiento'),
    ]) {
      testWidgets('muestra el mensaje del backend para ${code ?? 'un error sin código'}', (tester) async {
        server.on('POST', '/v1/auth/login', FakeResponse({'code': code, 'message': message}, status: 401));
        final router = await openLogin(tester);

        await login(tester);

        expect(find.text(message), findsOneWidget);
        expect(currentPath(router), '/screen');
      });
    }

    testWidgets('usa un mensaje propio cuando el backend no manda texto', (tester) async {
      server.on('POST', '/v1/auth/login', const FakeResponse({'code': 'INVALID_CREDENTIALS'}, status: 401));
      await openLogin(tester);

      await login(tester);

      expect(find.text('El PIN es incorrecto. Por favor, verifica e intenta de nuevo.'), findsOneWidget);
    });

    testWidgets('si el correo no está verificado lleva a verificarlo con el documento', (tester) async {
      server.on('POST', '/v1/auth/login', const FakeResponse({'code': 'USER_NOT_ACTIVE', 'message': 'x'}, status: 403));
      final router = await openLogin(tester);

      await tapVisible(tester, find.text('Extranjería (CE)'));
      await login(tester, document: '987654');
      expect(find.textContaining('Debes verificar tu correo'), findsOneWidget);

      await tester.pump(const Duration(seconds: 2));
      await tester.pumpAndSettle();

      expect(currentPath(router), '/verify-email');
      expect(currentExtra(router), {'documentType': 'CE', 'documentNumber': '987654'});
    });
  });

  group('dispositivo nuevo', () {
    setUp(() {
      server.on('POST', '/v1/auth/login', const FakeResponse(
          {'code': 'DEVICE_NOT_RECOGNIZED', 'message': 'Te enviamos un código a tu correo'}, status: 403));
    });

    testWidgets('pide el código del correo y reintenta el ingreso con él', (tester) async {
      final router = await openLogin(tester);
      await login(tester);
      expect(find.text('Nuevo dispositivo detectado'), findsOneWidget);

      await tester.enterText(find.byType(TextField).last, '4829');
      await tester.tap(find.text('Verificar'));
      await tester.pump();
      expect(find.text('Ingresa el código de 6 dígitos.'), findsOneWidget);

      server.on('POST', '/v1/auth/login', const FakeResponse(_tokens));
      await tester.enterText(find.byType(TextField).last, '482913');
      await tester.tap(find.text('Verificar'));
      await tester.pumpAndSettle();

      expect(loginRequests().last.json['otpCode'], '482913');
      expect(decryptWithTestPrivateKey(loginRequests().last.json['pin'] as String), '4821');
      expect(currentPath(router), '/home');
    });

    testWidgets('vuelve a pedir el código si el que se ingresó es incorrecto', (tester) async {
      await openLogin(tester);
      await login(tester);

      server.on('POST', '/v1/auth/login',
          const FakeResponse({'code': 'INVALID_OTP', 'message': 'El código no coincide'}, status: 403));
      await tester.enterText(find.byType(TextField).last, '000000');
      await tester.tap(find.text('Verificar'));
      await tester.pumpAndSettle();

      expect(find.text('Nuevo dispositivo detectado'), findsOneWidget);
      expect(find.text('El código no coincide'), findsOneWidget);
    });

    testWidgets('cancelar cierra el diálogo sin reintentar', (tester) async {
      await openLogin(tester);
      await login(tester);

      await tester.tap(find.text('Cancelar'));
      await tester.pumpAndSettle();

      expect(find.text('Nuevo dispositivo detectado'), findsNothing);
      expect(loginRequests(), hasLength(1));
    });
  });

  testWidgets('avisa que la sesión expiró cuando el servidor la cerró', (tester) async {
    await openLogin(tester, overrides: [sessionExpiredProvider.overrideWith((ref) => true)]);

    expect(find.text('Tu sesión expiró. Ingresa de nuevo con tu PIN.'), findsOneWidget);
    final container = ProviderScope.containerOf(tester.element(find.byType(LoginPage)));
    expect(container.read(sessionExpiredProvider), isFalse, reason: 'el aviso se muestra una sola vez');
  });

  testWidgets('abre el registro y la recuperación del PIN', (tester) async {
    final router = await openLogin(tester);

    await tapVisible(tester, find.text('¿No tienes cuenta? Regístrate'));
    await tester.pumpAndSettle();
    expect(currentPath(router), '/register');

    router.pop();
    await tester.pumpAndSettle();
    await tapVisible(tester, find.text('¿Olvidaste tu PIN?'));
    await tester.pumpAndSettle();
    expect(currentPath(router), '/auth/recover-pin');
  });
}
