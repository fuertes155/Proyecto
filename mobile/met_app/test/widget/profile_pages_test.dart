import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/features/auth/presentation/pages/personal_data_page.dart';
import 'package:met/features/auth/presentation/pages/profile_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

const _user = {
  'id': 'user-1',
  'firstName': 'Ana',
  'lastName': 'Pérez',
  'email': 'socio@example.com',
  'phone': '3001234567',
  'status': 'ACTIVE',
  'kycStatus': 'APPROVED',
};

/// Perfil del socio y sus datos personales, contra un servidor falso.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    mockNativePlugins(storage: {'access_token': 'acc-1', 'refresh_token': 'ref-1', 'user_id': 'user-1'});
    server = FakeDioAdapter()
      ..on('POST', '/v1/auth/logout', const FakeResponse({}))
      ..on('PUT', '/v1/auth/me', FakeResponse({..._user, 'email': 'nuevo@example.com', 'phone': '3109876543'}));
  });

  Future<void> tapAndSettle(WidgetTester tester, Finder finder) async {
    await tester.ensureVisible(finder);
    await tester.pumpAndSettle();
    await tester.tap(finder);
    await tester.pumpAndSettle();
  }

  group('Perfil', () {
    const options = {
      'Mis Datos Personales': '/profile/personal-data',
      'Seguridad y Contraseña': '/profile/security',
      'Cuentas bancarias externas': '/accounts/external',
      'Reportes': '/reports',
      'Ayuda y Soporte': '/support/chat',
    };

    Future<GoRouter> openProfile(WidgetTester tester, {Map<String, dynamic> user = _user}) async {
      final router = await pumpScreen(tester, const ProfilePage(),
          server: server, overrides: [signedIn(user)], destinations: {...options.values, '/login'});
      await tester.pumpAndSettle();
      return router;
    }

    testWidgets('muestra el nombre y el correo del socio', (tester) async {
      await openProfile(tester);

      expect(find.text('Ana Pérez'), findsOneWidget);
      expect(find.text('socio@example.com'), findsOneWidget);
    });

    testWidgets('invita a agregar el correo si la cuenta no tiene', (tester) async {
      await openProfile(tester, user: {..._user, 'email': ''});

      expect(find.text('Agrega tu correo en Mis Datos Personales'), findsOneWidget);
    });

    for (final MapEntry(key: option, value: route) in options.entries) {
      testWidgets('"$option" abre $route', (tester) async {
        final router = await openProfile(tester);

        await tapAndSettle(tester, find.text(option));

        expect(currentPath(router), route);
      });
    }

    testWidgets('cerrar sesión borra la sesión y lleva al ingreso', (tester) async {
      final router = await openProfile(tester);

      await tapAndSettle(tester, find.text('Cerrar Sesión'));

      expect(server.requests.single.path, '/v1/auth/logout');
      expect(await const FlutterSecureStorage().read(key: 'access_token'), isNull);
      expect(currentPath(router), '/login', reason: 'no debe quedarse en un perfil sin sesión');
    });
  });

  group('Datos personales', () {
    Future<void> openPersonalData(WidgetTester tester, {Map<String, dynamic> user = _user}) async {
      await pumpScreen(tester, const PersonalDataPage(), server: server, overrides: [signedIn(user)]);
      await tester.pumpAndSettle();
    }

    Future<void> fillContact(WidgetTester tester, {required String email, required String phone}) async {
      await tester.enterText(find.widgetWithText(TextFormField, 'Correo Electrónico'), email);
      await tester.enterText(find.widgetWithText(TextFormField, 'Número Celular (10 dígitos)'), phone);
      await tester.pumpAndSettle();
      await tapAndSettle(tester, find.text('Guardar datos de contacto'));
    }

    testWidgets('muestra los datos de solo lectura y copia el ID de la cuenta', (tester) async {
      String? copied;
      tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(SystemChannels.platform, (call) async {
        if (call.method == 'Clipboard.setData') copied = (call.arguments as Map)['text'] as String;
        return null;
      });
      await openPersonalData(tester);

      expect(find.text('Ana Pérez'), findsOneWidget);
      expect(find.text('socio@example.com'), findsOneWidget);
      expect(find.text('3001234567'), findsOneWidget);
      expect(find.text('Guardar datos de contacto'), findsNothing);

      await tapAndSettle(tester, find.byIcon(Icons.copy));
      expect(copied, 'user-1');
      expect(find.text('ID copiado al portapapeles'), findsOneWidget);
    });

    testWidgets('pide el correo y el celular una sola vez y luego los deja de solo lectura', (tester) async {
      await openPersonalData(tester, user: {..._user, 'email': '', 'phone': ''});

      await fillContact(tester, email: 'nuevo@example.com', phone: '3109876543');
      expect(find.text('Confirma tu correo'), findsOneWidget);
      await tapAndSettle(tester, find.text('Guardar'));

      expect(server.last.json, {'email': 'nuevo@example.com', 'phone': '3109876543'});
      expect(find.text('Datos de contacto guardados.'), findsOneWidget);
      expect(find.text('Guardar datos de contacto'), findsNothing);
      expect(find.text('nuevo@example.com'), findsOneWidget);
    });

    testWidgets('valida el correo y el celular colombiano antes de enviar', (tester) async {
      await openPersonalData(tester, user: {..._user, 'email': '', 'phone': ''});

      await fillContact(tester, email: 'socio@', phone: '2001234567');

      expect(find.text('Correo inválido'), findsOneWidget);
      expect(find.text('Celular inválido'), findsOneWidget);
      expect(server.requests, isEmpty);
    });

    testWidgets('"Corregir" no guarda nada', (tester) async {
      await openPersonalData(tester, user: {..._user, 'email': '', 'phone': ''});

      await fillContact(tester, email: 'nuevo@example.com', phone: '3109876543');
      await tapAndSettle(tester, find.text('Corregir'));

      expect(server.requests, isEmpty);
      expect(find.text('Guardar datos de contacto'), findsOneWidget);
    });

    testWidgets('muestra el motivo si el backend rechaza el correo', (tester) async {
      server.on('PUT', '/v1/auth/me', const FakeResponse({'code': 'EMAIL_TAKEN', 'message': 'Ese correo ya está registrado'}, status: 409));
      await openPersonalData(tester, user: {..._user, 'email': '', 'phone': ''});

      await fillContact(tester, email: 'nuevo@example.com', phone: '3109876543');
      await tapAndSettle(tester, find.text('Guardar'));

      expect(find.text('Ese correo ya está registrado'), findsOneWidget);
      expect(find.text('Guardar datos de contacto'), findsOneWidget);
    });
  });
}
