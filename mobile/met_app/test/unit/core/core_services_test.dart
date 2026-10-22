import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met/core/network/api_client_provider.dart';
import 'package:met/core/security/rsa_encryption_service.dart';
import 'package:met/core/storage/secure_storage_service.dart';
import 'package:met/core/utils/currency_formatter.dart';
import 'package:met/core/utils/error_mapper.dart';
import 'package:met/features/auth/data/models/auth_models.dart';
import 'package:met/features/auth/presentation/providers/auth_provider.dart';
import 'package:met/features/support/presentation/providers/chat_provider.dart';

import '../../helpers/fake_dio.dart';
import '../../helpers/test_rsa_keys.dart';

const _user = {
  'id': 'user-1',
  'firstName': 'Ana',
  'lastName': 'Pérez',
  'email': 'socio@example.com',
  'phone': '3001234567',
  'status': 'ACTIVE',
  'kycStatus': 'APPROVED',
  'emailNotificationsEnabled': true,
  'pushNotificationsEnabled': true,
};

DioException _dioError(DioExceptionType type, {int? status, Object? data}) {
  final options = RequestOptions(path: '/v1/x');
  return DioException(
    requestOptions: options,
    type: type,
    response: status == null ? null : Response(requestOptions: options, statusCode: status, data: data),
  );
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('ErrorMapper', () {
    test('explica los problemas de red con palabras del usuario', () {
      expect(ErrorMapper.toUserMessage(_dioError(DioExceptionType.connectionTimeout)), contains('Tiempo de conexión agotado'));
      expect(ErrorMapper.toUserMessage(_dioError(DioExceptionType.receiveTimeout)), contains('Tiempo de conexión agotado'));
      expect(ErrorMapper.toUserMessage(_dioError(DioExceptionType.connectionError)), contains('Sin conexión al servidor'));
      expect(ErrorMapper.toUserMessage(_dioError(DioExceptionType.cancel)), 'La solicitud fue cancelada.');
      expect(ErrorMapper.toUserMessage(_dioError(DioExceptionType.unknown)), 'Error de red. Intenta de nuevo.');
    });

    test('prefiere el mensaje del backend y si no hay, uno según el código HTTP', () {
      expect(ErrorMapper.toUserMessage(_dioError(DioExceptionType.badResponse, status: 422, data: {'message': 'Saldo insuficiente'})),
          'Saldo insuficiente');
      final expected = {
        400: 'Datos inválidos',
        401: 'Sesión expirada',
        403: 'No tienes permiso',
        404: 'no fue encontrado',
        429: 'Demasiados intentos',
        503: 'Error en el servidor',
        418: 'código 418',
      };
      expected.forEach((status, text) {
        expect(ErrorMapper.toUserMessage(_dioError(DioExceptionType.badResponse, status: status, data: '<html>')), contains(text),
            reason: 'HTTP $status');
      });
    });

    test('muestra textos de negocio cortos y oculta los técnicos', () {
      expect(ErrorMapper.toUserMessage('El monto supera tu límite'), 'El monto supera tu límite');
      expect(ErrorMapper.toUserMessage(Exception('NullPointer en algo interno')), 'Ocurrió un error inesperado. Intenta de nuevo.');
      expect(ErrorMapper.toUserMessage('x' * 250), 'Ocurrió un error inesperado. Intenta de nuevo.');
    });
  });

  group('Formato de pesos', () {
    test('usa punto de miles y coma decimal como en Colombia', () {
      expect(formatPesos(2450000), '\$ 2.450.000');
      expect(formatPesos(38750.5, cents: true), '\$ 38.750,50');
      expect(formatPesos(-1500), '-\$ 1.500');
      expect(formatPesos(0.005, cents: true), '\$ 0,01', reason: 'redondea al centavo');
    });
  });

  group('Cifrado del PIN', () {
    test('cifra con la llave pública del servidor y la pide una sola vez', () async {
      final server = FakeDioAdapter()..on('GET', '/v1/auth/public-key', const FakeResponse({'publicKey': testRsaPublicKeyBase64}));
      final rsa = RsaEncryptionService(fakeDio(server));

      final first = await rsa.encryptPin('4821');
      final second = await rsa.encryptPin('9375');

      expect(decryptWithTestPrivateKey(first), '4821');
      expect(decryptWithTestPrivateKey(second), '9375');
      expect(server.requests.length, 1);
    });

    test('acepta la llave como texto plano o como JSON en texto', () async {
      final plain = FakeDioAdapter()..on('GET', '/v1/auth/public-key', const FakeResponse(testRsaPublicKeyBase64));
      final jsonText = FakeDioAdapter()
        ..on('GET', '/v1/auth/public-key', const FakeResponse('{"publicKey":"$testRsaPublicKeyBase64"}'));

      expect(decryptWithTestPrivateKey(await RsaEncryptionService(fakeDio(plain)).encryptPin('1111')), '1111');
      expect(decryptWithTestPrivateKey(await RsaEncryptionService(fakeDio(jsonText)).encryptPin('2222')), '2222');
    });

    test('avisa cuando la URL de la API apunta a una página web en vez del backend', () async {
      final server = FakeDioAdapter()..on('GET', '/v1/auth/public-key', const FakeResponse('<!DOCTYPE html><html></html>'));

      await expectLater(RsaEncryptionService(fakeDio(server)).encryptPin('4821'),
          throwsA(predicate((e) => '$e'.contains('API_BASE_URL'))));
    });

    test('propaga los errores de red tal cual para que ErrorMapper los traduzca', () async {
      final server = FakeDioAdapter()..on('GET', '/v1/auth/public-key', const FakeResponse({}, status: 503));

      await expectLater(RsaEncryptionService(fakeDio(server)).encryptPin('4821'), throwsA(isA<DioException>()));
    });

    test('rechaza una respuesta sin llave', () async {
      final server = FakeDioAdapter()..on('GET', '/v1/auth/public-key', const FakeResponse({'otraCosa': 1}));

      await expectLater(RsaEncryptionService(fakeDio(server)).encryptPin('4821'), throwsA(isA<Exception>()));
    });

    test('no llama al servidor para un PIN vacío', () async {
      final server = FakeDioAdapter();

      expect(await RsaEncryptionService(fakeDio(server)).encryptPin(''), '');
      expect(server.requests, isEmpty);
    });
  });

  group('Asistente de chat', () {
    late FakeDioAdapter server;
    late ProviderContainer container;

    setUp(() {
      server = FakeDioAdapter();
      container = ProviderContainer(overrides: [apiClientProvider.overrideWithValue(fakeDio(server))]);
    });

    tearDown(() => container.dispose());

    test('envía la pregunta y muestra la respuesta del asistente', () async {
      server.on('POST', '/support/chat', const FakeResponse({'reply': 'Ahorra un monto fijo cada semana.'}));

      await container.read(chatProvider.notifier).sendMessage('¿Cómo ahorro más?');

      final state = container.read(chatProvider);
      expect(state.messages.last.text, 'Ahorra un monto fijo cada semana.');
      expect(state.messages[1].isUser, isTrue);
      expect(state.showQuickOptions, isFalse);
      expect(state.isLoading, isFalse);
    });

    test('responde por palabra clave cuando el servidor no contesta', () async {
      server.on('POST', '/support/chat', const FakeResponse({}, status: 503));
      final chat = container.read(chatProvider.notifier);

      await chat.sendMessage('¿Cuál es mi saldo?');
      expect(container.read(chatProvider).messages.last.text, contains('ahorro principal'));
      await chat.sendMessage('Quiero un préstamo');
      expect(container.read(chatProvider).messages.last.text, contains('Créditos'));
      await chat.sendMessage('Necesito transferir');
      expect(container.read(chatProvider).messages.last.text, contains('Transferir'));
      await chat.sendMessage('teléfono de contacto');
      expect(container.read(chatProvider).messages.last.text, contains('01 8000'));
      await chat.sendMessage('hola');
      expect(container.read(chatProvider).messages.last.text, contains('WhatsApp'));
    });

    test('ignora mensajes vacíos y contesta las opciones rápidas sin ir al servidor', () async {
      final chat = container.read(chatProvider.notifier);

      await chat.sendMessage('   ');
      await chat.sendQuickOption('❓ Preguntas frecuentes', 'faq');
      await chat.sendQuickOption('Otra', 'no-existe');

      final messages = container.read(chatProvider).messages;
      expect(messages[1].category, 'faq');
      expect(messages[2].text, contains('Olvidé PIN'));
      expect(messages.last.text, contains('Lo siento'));
      expect(server.requests, isEmpty);
    });
  });

  group('Estado de sesión', () {
    late FakeDioAdapter server;
    late ProviderContainer container;

    setUp(() {
      FlutterSecureStorage.setMockInitialValues({});
      server = FakeDioAdapter()
        ..on('GET', '/v1/auth/public-key', const FakeResponse({'publicKey': testRsaPublicKeyBase64}))
        ..on('GET', '/v1/auth/me', const FakeResponse(_user))
        ..on('POST', '/v1/auth/login', const FakeResponse(
            {'userId': 'user-1', 'accessToken': 'acc', 'refreshToken': 'ref', 'expiresInMs': 1800000}));
      container = ProviderContainer(overrides: [apiClientProvider.overrideWithValue(fakeDio(server))]);
    });

    tearDown(() => container.dispose());

    AuthNotifier auth() => container.read(authStateProvider.notifier);

    test('sin sesión guardada queda deslogueado sin consultar el perfil', () async {
      await auth().checkSession();

      expect(container.read(authStateProvider).value, isNull);
      expect(server.requests.any((r) => r.path == '/v1/auth/me'), isFalse);
    });

    test('ingresa enviando el id del dispositivo y carga el perfil', () async {
      await auth().loginWithPin(LoginRequest(documentType: 'CC', documentNumber: '1086578123', pin: '4821'));

      final deviceId = await container.read(secureStorageProvider).getDeviceId();
      expect(server.requests.firstWhere((r) => r.path == '/v1/auth/login').json['deviceId'], deviceId);
      expect(container.read(authStateProvider).value!.firstName, 'Ana');

      await auth().checkSession();
      expect(container.read(authStateProvider).value!.kycStatus, 'APPROVED', reason: 'la sesión quedó guardada');
    });

    test('ingresa con huella y expone el error si el servidor rechaza', () async {
      await auth().loginWithBiometric(LoginRequest(documentType: 'CC', documentNumber: '1', biometricPayload: 'bio'));
      expect(container.read(authStateProvider).hasValue, isTrue);

      server.on('POST', '/v1/auth/login', const FakeResponse({'message': 'Credenciales inválidas'}, status: 422));
      await auth().loginWithPin(LoginRequest(documentType: 'CC', documentNumber: '1', pin: '0000'));
      expect(container.read(authStateProvider).hasError, isTrue);
    });

    test('cierra sesión en el servidor o solo localmente cuando el token ya no sirve', () async {
      server.on('POST', '/v1/auth/logout', const FakeResponse({}));
      await auth().loginWithPin(LoginRequest(documentType: 'CC', documentNumber: '1', pin: '4821'));

      await auth().forceLocalLogout();
      expect(container.read(authStateProvider).value, isNull);
      expect(server.requests.any((r) => r.path == '/v1/auth/logout'), isFalse);

      await auth().logout();
      expect(server.last.path, '/v1/auth/logout');
    });

    test('actualiza perfil y notificaciones y deja pasar los errores a la pantalla', () async {
      server
        ..on('PUT', '/v1/auth/me', FakeResponse({..._user, 'email': 'nuevo@example.com'}))
        ..on('PUT', '/v1/auth/notifications', FakeResponse({..._user, 'emailNotificationsEnabled': false}))
        ..on('PUT', '/v1/auth/pin', const FakeResponse({'message': 'PIN actual incorrecto'}, status: 422));

      await auth().updateProfile(email: 'nuevo@example.com', phone: '3001234567');
      expect(container.read(authStateProvider).value!.email, 'nuevo@example.com');
      await auth().updateNotifications(emailEnabled: false, pushEnabled: true);
      expect(container.read(authStateProvider).value!.emailNotificationsEnabled, isFalse);
      await expectLater(auth().updatePin(currentPin: '0000', newPin: '1234'), throwsA(isA<DioException>()));
    });
  });
}
