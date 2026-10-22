import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/features/auth/presentation/pages/biometric_registration_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

/// PNG de 1×1 para las "fotos" que devuelve la cámara simulada.
final _pixelPng = base64Decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=');

const _session = {'access_token': 'acc-1', 'refresh_token': 'ref-1', 'user_id': 'user-1'};

const _user = {
  'id': 'user-1',
  'firstName': 'Ana',
  'lastName': 'Pérez',
  'email': 'socio@example.com',
  'phone': '3001234567',
  'status': 'ACTIVE',
  'kycStatus': 'IN_REVIEW',
  'emailNotificationsEnabled': true,
  'pushNotificationsEnabled': false,
};

/// Verificación de identidad: tres fotos con la cámara, la firma dibujada y el envío al backend.
void main() {
  late FakeDioAdapter server;
  late Directory photos;
  late List<Object?> cameraResults;

  setUpAll(() => photos = Directory.systemTemp.createTempSync('met_kyc_'));
  tearDownAll(() => photos.deleteSync(recursive: true));

  String photo(String name, [List<int>? bytes]) {
    final file = File('${photos.path}/$name')..writeAsBytesSync(bytes ?? _pixelPng);
    return file.path;
  }

  setUp(() {
    mockNativePlugins(storage: _session);
    cameraResults = [];
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('plugins.flutter.io/image_picker'), (call) async {
      final next = cameraResults.removeAt(0);
      if (next is PlatformException) throw next;
      return next;
    });
    server = FakeDioAdapter()
      ..on('POST', '/v1/auth/biometric', const FakeResponse({}))
      ..on('GET', '/v1/auth/me', const FakeResponse(_user));
  });

  Future<GoRouter> openPage(WidgetTester tester) async {
    final router = await pumpScreen(tester, const BiometricRegistrationPage(),
        server: server, destinations: {'/home', '/login'});
    await tester.pumpAndSettle();
    return router;
  }

  /// Leer la foto del disco y dibujar la firma en PNG son operaciones reales (no del reloj falso de
  /// la prueba): se les da tiempo real y luego se procesa lo que dejaron pendiente.
  Future<void> letRealWorkFinish(WidgetTester tester) async {
    for (var i = 0; i < 10; i++) {
      await tester.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 20)));
      await tester.pump();
    }
    await tester.pumpAndSettle();
  }

  Future<void> tapVisible(WidgetTester tester, Finder finder) async {
    await tester.ensureVisible(finder);
    await tester.pumpAndSettle();
    await tester.tap(finder);
  }

  Future<void> takePhoto(WidgetTester tester, String slot, String path) async {
    cameraResults.add(path);
    await tapVisible(tester, find.text(slot));
    await letRealWorkFinish(tester);
  }

  Future<void> takeAllPhotos(WidgetTester tester) async {
    await takePhoto(tester, 'Cédula Original', photo('frente.jpg'));
    await takePhoto(tester, 'Cédula (reverso)', photo('reverso.png'));
    await takePhoto(tester, 'Selfie', photo('selfie.jpeg'));
  }

  Future<void> sign(WidgetTester tester) async {
    await tester.ensureVisible(find.text('Dibuja tu firma aquí'));
    await tester.pumpAndSettle();
    await tester.drag(find.text('Dibuja tu firma aquí'), const Offset(120, 30));
    await tester.pumpAndSettle();
  }

  ElevatedButton submitButton(WidgetTester tester) =>
      tester.widget<ElevatedButton>(find.widgetWithText(ElevatedButton, 'Completar Verificación'));

  testWidgets('solo deja enviar con las tres fotos y la firma', (tester) async {
    await openPage(tester);
    expect(submitButton(tester).onPressed, isNull);

    await takeAllPhotos(tester);
    expect(find.text('Completado'), findsNWidgets(3));
    expect(submitButton(tester).onPressed, isNull, reason: 'falta la firma');

    await sign(tester);
    expect(submitButton(tester).onPressed, isNotNull);

    await tapVisible(tester, find.text('Limpiar firma'));
    await tester.pumpAndSettle();
    expect(find.text('Dibuja tu firma aquí'), findsOneWidget);
    expect(submitButton(tester).onPressed, isNull);
  });

  testWidgets('envía las fotos y la firma y abre el inicio', (tester) async {
    final router = await openPage(tester);
    await takeAllPhotos(tester);
    await sign(tester);

    await tapVisible(tester, find.text('Completar Verificación'));
    await letRealWorkFinish(tester);

    final sent = server.requests.singleWhere((r) => r.path == '/v1/auth/biometric').json;
    expect(sent['userId'], 'user-1', reason: 'la sesión se recupera del almacenamiento seguro');
    expect(base64Decode(sent['documentImageBase64'] as String), _pixelPng);
    expect(base64Decode(sent['documentBackImageBase64'] as String), _pixelPng);
    expect(base64Decode(sent['selfieImageBase64'] as String), _pixelPng);
    final signature = base64Decode(sent['signatureImageBase64'] as String);
    expect(signature.sublist(1, 4), utf8.encode('PNG'));
    expect(currentPath(router), '/home');
  });

  testWidgets('rechaza archivos que no son fotos', (tester) async {
    await openPage(tester);

    await takePhoto(tester, 'Cédula Original', photo('cedula.pdf', utf8.encode('%PDF-1.7')));

    expect(find.textContaining('No se permiten archivos PDF'), findsOneWidget);
    expect(find.text('Completado'), findsNothing);
  });

  testWidgets('explica cómo dar el permiso de cámara si se negó', (tester) async {
    await openPage(tester);

    cameraResults.add(PlatformException(code: 'camera_access_denied'));
    await tapVisible(tester, find.text('Selfie'));
    await letRealWorkFinish(tester);

    expect(find.textContaining('Necesitamos permiso de cámara'), findsOneWidget);
  });

  testWidgets('no hace nada si el socio cierra la cámara sin tomar la foto', (tester) async {
    await openPage(tester);

    cameraResults.add(null);
    await tapVisible(tester, find.text('Selfie'));
    await letRealWorkFinish(tester);

    expect(find.text('Completado'), findsNothing);
    expect(find.byType(SnackBar), findsNothing);
  });

  testWidgets('lleva al login si la sesión ya no existe', (tester) async {
    mockNativePlugins();
    final router = await openPage(tester);
    await takeAllPhotos(tester);
    await sign(tester);

    await tapVisible(tester, find.text('Completar Verificación'));
    await letRealWorkFinish(tester);

    expect(server.requests.where((r) => r.path == '/v1/auth/biometric'), isEmpty);
    expect(currentPath(router), '/login');
  });

  testWidgets('muestra el motivo si el backend rechaza la verificación', (tester) async {
    server.on('POST', '/v1/auth/biometric',
        const FakeResponse({'code': 'FACE_MISMATCH', 'message': 'La selfie no coincide con la cédula'}, status: 422));
    final router = await openPage(tester);
    await takeAllPhotos(tester);
    await sign(tester);

    await tapVisible(tester, find.text('Completar Verificación'));
    await letRealWorkFinish(tester);

    expect(find.text('Error: La selfie no coincide con la cédula'), findsOneWidget);
    expect(currentPath(router), '/screen');
    expect(submitButton(tester).onPressed, isNotNull, reason: 'puede volver a intentarlo');
  });

  testWidgets('la flecha atrás vuelve al inicio de sesión', (tester) async {
    final router = await openPage(tester);

    await tester.tap(find.byTooltip('Volver a inicio de sesión'));
    await tester.pumpAndSettle();

    expect(currentPath(router), '/login');
  });
}
