import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import '../helpers/fake_dio.dart';
import '../helpers/panel_harness.dart';

/// Ingreso de administradores, incluido el bloqueo tras varios intentos fallidos.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    server = FakeDioAdapter()
      ..on('POST', '/v1/admin/auth/login', const FakeResponse({
        'adminId': 'admin-1', 'username': 'jperez', 'fullName': 'Juan Pérez', 'role': 'SUPER_ADMIN',
        'accessToken': 'admin-token-nuevo', 'expiresInMs': 1800000,
      }))
      ..on('GET', '/users', const FakeResponse([]))
      ..on('GET', '/accounts', const FakeResponse([]))
      ..on('GET', '/loans', const FakeResponse([]))
      ..on('GET', '/v1/admin/investments/guarantee-fund',
          const FakeResponse({'balance': 0, 'coverageRatio': 0.8, 'activationDaysLate': 90, 'movements': []}))
      ..on('GET', '/v1/admin/maintenance', const FakeResponse([]))
      ..on('GET', '/v1/admin/compliance/alerts?status=OPEN&page=0&size=20', const FakeResponse({'items': [], 'total': 0}));
  });

  Future<void> logIn(WidgetTester tester, {String user = 'jperez', String password = 'Clave-Segura-1'}) async {
    final fields = find.byType(TextFormField);
    await tester.enterText(fields.at(0), user);
    await tester.enterText(fields.at(1), password);
    await tapAndSettle(tester, find.text('Iniciar sesión'));
  }

  testWidgets('entra al tablero y guarda el token del administrador', (tester) async {
    final container = await pumpPanel(tester, '/admin/login', server: server);

    await logIn(tester, user: '  jperez ');

    expect(requestsTo(server, '/v1/admin/auth/login').single.json, {'username': 'jperez', 'password': 'Clave-Segura-1'});
    expect(await const FlutterSecureStorage().read(key: 'admin_access_token'), 'admin-token-nuevo');
    expect(panelPath(container), '/admin/dashboard');
  });

  testWidgets('con datos incorrectos lo explica, borra la contraseña y se queda', (tester) async {
    server.on('POST', '/v1/admin/auth/login',
        const FakeResponse({'code': 'INVALID_CREDENTIALS', 'message': 'Credenciales inválidas'}, status: 401));
    final container = await pumpPanel(tester, '/admin/login', server: server);

    await logIn(tester);

    expect(find.text('Usuario o contraseña incorrectos. Revisa los datos e inténtalo de nuevo.'), findsOneWidget);
    expect(tester.widget<TextFormField>(find.byType(TextFormField).at(1)).controller!.text, isEmpty);
    expect(panelPath(container), '/admin/login');
  });

  testWidgets('tras varios intentos muestra el bloqueo temporal del backend', (tester) async {
    server.on('POST', '/v1/admin/auth/login', const FakeResponse({
      'code': 'ADMIN_LOCKED',
      'message': 'La cuenta está bloqueada temporalmente por múltiples intentos fallidos. Intenta en 15 minutos.',
    }, status: 422));
    await pumpPanel(tester, '/admin/login', server: server);

    await logIn(tester);

    expect(find.textContaining('bloqueada temporalmente'), findsOneWidget);
  });

  testWidgets('ante el límite de peticiones pide esperar', (tester) async {
    server.on('POST', '/v1/admin/auth/login', const FakeResponse('Too Many Requests', status: 429));
    await pumpPanel(tester, '/admin/login', server: server);

    await logIn(tester);

    expect(find.text('Demasiados intentos. Espera un minuto antes de volver a intentar.'), findsOneWidget);
  });

  testWidgets('valida usuario y contraseña antes de enviar', (tester) async {
    await pumpPanel(tester, '/admin/login', server: server);

    await logIn(tester, user: '', password: 'corta');

    expect(find.text('La contraseña tiene al menos 8 caracteres'), findsOneWidget);
    expect(requestsTo(server, '/v1/admin/auth/login'), isEmpty);
  });
}
