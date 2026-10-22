import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met/core/network/api_client_provider.dart';
import 'package:met/core/storage/secure_storage_service.dart';
import 'package:met/features/auth/data/models/auth_models.dart';
import 'package:met/features/auth/data/repositories/auth_repository_impl.dart';
import 'package:met/features/auth/domain/repositories/auth_repository.dart';
import 'package:met/features/auth/presentation/providers/pin_recovery_provider.dart';

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
  'pushNotificationsEnabled': false,
};

/// Ingreso, registro, sesión y recuperación de PIN de punta a punta contra un servidor falso, con
/// el almacenamiento seguro real (en memoria): lo único simulado es la red y el llavero del sistema.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late FakeDioAdapter server;
  late ProviderContainer container;
  late AuthRepository auth;
  late SecureStorageService storage;

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
    server = FakeDioAdapter();
    server.on('GET', '/v1/auth/public-key', const FakeResponse({'publicKey': testRsaPublicKeyBase64}));
    container = ProviderContainer(overrides: [apiClientProvider.overrideWithValue(fakeDio(server))]);
    auth = container.read(authRepositoryProvider);
    storage = container.read(secureStorageProvider);
  });

  tearDown(() => container.dispose());

  group('Sesión', () {
    test('ingresa con PIN cifrado y guarda los tokens de la sesión', () async {
      server.on('POST', '/v1/auth/login', const FakeResponse(
          {'userId': 'user-1', 'accessToken': 'acc-1', 'refreshToken': 'ref-1', 'expiresInMs': 1800000}));
      expect(await auth.hasSession(), isFalse);

      final response = await auth.loginWithPin(LoginRequest(
          documentType: 'CC', documentNumber: '1086578123', pin: '4821', deviceId: 'device-1'));

      final sent = server.last.json;
      expect(sent['documentNumber'], '1086578123');
      expect(sent['deviceId'], 'device-1');
      expect(decryptWithTestPrivateKey(sent['pin'] as String), '4821');
      expect(response.expiresInMs, 1800000);
      expect(await storage.readAccessToken(), 'acc-1');
      expect(await storage.readRefreshToken(), 'ref-1');
      expect(await storage.readUserId(), 'user-1');
      expect(await auth.hasSession(), isTrue);
    });

    test('ingresa con huella sin enviar PIN', () async {
      server.on('POST', '/v1/auth/login', const FakeResponse(
          {'userId': 'user-1', 'accessToken': 'acc-2', 'refreshToken': 'ref-2', 'expiresInMs': 1800000}));

      await auth.loginWithBiometric(LoginRequest(documentType: 'CC', documentNumber: '1086578123', biometricPayload: 'bio-token'));

      expect(server.last.json.containsKey('pin'), isFalse);
      expect(server.last.json['biometricPayload'], 'bio-token');
      expect(server.requests.any((r) => r.path == '/v1/auth/public-key'), isFalse, reason: 'sin PIN no hace falta la llave');
      expect(await storage.readAccessToken(), 'acc-2');
    });

    test('al cerrar sesión borra los tokens aunque el servidor falle, pero conserva lo del dispositivo', () async {
      await storage.saveTokens(accessToken: 'acc', refreshToken: 'ref', userId: 'user-1');
      final deviceId = await storage.getDeviceId();
      await storage.saveBiometricCredential(token: 'bio', documentType: 'CC', documentNumber: '1086578123');
      server.on('POST', '/v1/auth/logout', const FakeResponse({}, status: 500));

      await auth.logout();

      expect(await auth.hasSession(), isFalse);
      expect(await storage.readRefreshToken(), isNull);
      expect(await storage.getDeviceId(), deviceId, reason: 'si cambiara, cada ingreso pediría código de dispositivo nuevo');
      expect((await storage.readBiometricCredential())!.documentNumber, '1086578123');
    });

    test('cierra la sesión tras 15 minutos de inactividad', () async {
      expect(await storage.isSessionExpired(), isFalse, reason: 'sin registro de actividad no hay nada que vencer');

      await storage.saveLastActivity();
      expect(await storage.isSessionExpired(), isFalse);

      await storage.write(key: 'last_activity_at',
          value: DateTime.now().subtract(const Duration(minutes: 16)).millisecondsSinceEpoch.toString());
      expect(await storage.isSessionExpired(), isTrue);
    });

    test('olvida la credencial de huella cuando se desactiva', () async {
      await storage.saveBiometricCredential(token: 'bio', documentType: 'CC', documentNumber: '1');
      await storage.deleteBiometricCredential();

      expect(await storage.readBiometricCredential(), isNull);
    });
  });

  group('Cuenta', () {
    test('registra al socio con el PIN cifrado', () async {
      server.on('POST', '/v1/auth/register', FakeResponse({..._user, 'status': 'PENDING_VERIFICATION'}));

      final user = await auth.register(RegisterRequest(
          documentType: 'CC', documentNumber: '1086578123', fullName: 'Ana Pérez', pin: '4821', termsAccepted: true));

      expect(decryptWithTestPrivateKey(server.last.json['pin'] as String), '4821');
      expect(server.last.json['termsAccepted'], isTrue);
      expect(user.status, 'PENDING_VERIFICATION');
    });

    test('cambia el PIN cifrando el actual y el nuevo', () async {
      server.on('PUT', '/v1/auth/pin', const FakeResponse({}));

      await auth.updatePin(currentPin: '4821', newPin: '9375');

      expect(decryptWithTestPrivateKey(server.last.json['currentPin'] as String), '4821');
      expect(decryptWithTestPrivateKey(server.last.json['newPin'] as String), '9375');
    });

    test('activa y desactiva el ingreso con huella', () async {
      server
        ..on('PUT', '/v1/auth/biometric-login', const FakeResponse({}))
        ..on('DELETE', '/v1/auth/biometric-login', const FakeResponse({}));

      await auth.enableBiometricLogin(pin: '4821', biometricToken: 'bio-token');
      expect(decryptWithTestPrivateKey(server.last.json['pin'] as String), '4821');
      expect(server.last.json['biometricToken'], 'bio-token');

      await auth.disableBiometricLogin();
      expect(server.last.method, 'DELETE');
    });

    test('lee y actualiza el perfil y las notificaciones', () async {
      server
        ..on('GET', '/v1/auth/me', const FakeResponse(_user))
        ..on('PUT', '/v1/auth/me', FakeResponse({..._user, 'email': 'nuevo@example.com'}))
        ..on('PUT', '/v1/auth/notifications', FakeResponse({..._user, 'pushNotificationsEnabled': true}));

      expect((await auth.getProfile()).firstName, 'Ana');
      expect((await auth.updateProfile(email: 'nuevo@example.com', phone: '3001234567')).email, 'nuevo@example.com');
      expect(server.last.json, {'email': 'nuevo@example.com', 'phone': '3001234567'});
      expect((await auth.updateNotifications(emailEnabled: false, pushEnabled: true)).pushNotificationsEnabled, isTrue);
      expect(server.last.json, {'emailNotificationsEnabled': false, 'pushNotificationsEnabled': true});
    });
  });

  group('Recuperación de PIN', () {
    setUp(() => container.listen(pinRecoveryProvider, (_, __) {}));

    PinRecoveryNotifier recovery() => container.read(pinRecoveryProvider.notifier);
    PinRecoveryState recoveryState() => container.read(pinRecoveryProvider);

    test('pide el código y luego cambia el PIN con el documento recordado', () async {
      server
        ..on('POST', '/v1/auth/pin-recovery/request', const FakeResponse({}))
        ..on('POST', '/v1/auth/pin-recovery/reset', const FakeResponse({}));

      expect(await recovery().requestRecovery('CE', '987654'), isTrue);
      expect(recoveryState().currentStep, 1);

      expect(await recovery().resetPin('482913', '9375'), isTrue);
      final sent = server.last.json;
      expect(sent['documentType'], 'CE');
      expect(sent['documentNumber'], '987654');
      expect(sent['otpCode'], '482913');
      expect(decryptWithTestPrivateKey(sent['newPin'] as String), '9375');
    });

    test('muestra el mensaje del backend cuando el código es incorrecto', () async {
      server.on('POST', '/v1/auth/pin-recovery/reset',
          const FakeResponse({'code': 'INVALID_OTP', 'message': 'El código es incorrecto o expiró'}, status: 422));

      expect(await recovery().resetPin('000000', '9375'), isFalse);

      expect(recoveryState().error, 'El código es incorrecto o expiró');
      expect(recoveryState().isLoading, isFalse);
    });

    test('no se queda cargando si el servidor responde algo que no es JSON', () async {
      server
        ..on('POST', '/v1/auth/pin-recovery/request', const FakeResponse('<html>502</html>', status: 502))
        ..on('POST', '/v1/auth/pin-recovery/reset', const FakeResponse('Bad Gateway', status: 502));

      expect(await recovery().requestRecovery('CC', '1086578123'), isFalse);
      expect(recoveryState().isLoading, isFalse);
      expect(recoveryState().error, isNotNull);

      expect(await recovery().resetPin('482913', '9375'), isFalse);
      expect(recoveryState().isLoading, isFalse);
      expect(recoveryState().error, isNotNull);
    });

    test('mantiene los pasos entre 0 y 2', () {
      recovery()
        ..previousStep()
        ..nextStep()
        ..nextStep()
        ..nextStep();
      expect(recoveryState().currentStep, 2);
    });
  });
}
