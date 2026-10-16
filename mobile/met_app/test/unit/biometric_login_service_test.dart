import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met/core/storage/secure_storage_service.dart';
import 'package:met/features/auth/data/services/biometric_login_service.dart';
import 'package:met/features/auth/domain/repositories/auth_repository.dart';
import 'package:mocktail/mocktail.dart';

class MockAuthRepository extends Mock implements AuthRepository {}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late Map<String, String> stored;
  late bool fingerprintAccepted;
  late List<String> enrolledBiometrics;
  late MockAuthRepository repository;
  late BiometricLoginService service;

  setUp(() {
    stored = {};
    fingerprintAccepted = true;
    enrolledBiometrics = ['fingerprint'];
    final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
    messenger.setMockMethodCallHandler(const MethodChannel('plugins.it_nomads.com/flutter_secure_storage'), (call) async {
      final args = (call.arguments as Map?) ?? const {};
      switch (call.method) {
        case 'read':
          return stored[args['key']];
        case 'write':
          stored[args['key'] as String] = args['value'] as String;
        case 'delete':
          stored.remove(args['key']);
        case 'deleteAll':
          stored.clear();
      }
      return null;
    });
    messenger.setMockMethodCallHandler(const MethodChannel('plugins.flutter.io/local_auth'), (call) async {
      switch (call.method) {
        case 'isDeviceSupported':
          return true;
        case 'getAvailableBiometrics':
          return enrolledBiometrics;
        case 'authenticate':
          return fingerprintAccepted;
      }
      return null;
    });

    repository = MockAuthRepository();
    when(() => repository.enableBiometricLogin(pin: any(named: 'pin'), biometricToken: any(named: 'biometricToken')))
        .thenAnswer((_) async {});
    when(() => repository.disableBiometricLogin()).thenAnswer((_) async {});
    service = BiometricLoginService(storage: SecureStorageService(), repository: repository);
  });

  test('is available only when the phone has a fingerprint or face enrolled', () async {
    expect(await service.isAvailable(), isTrue);

    enrolledBiometrics = [];
    expect(await service.isAvailable(), isFalse);
  });

  test('enable confirms the fingerprint, sends the PIN with a new random key and keeps the key on the phone', () async {
    final activated = await service.enable(pin: '1234', documentType: 'CE', documentNumber: '456789');

    expect(activated, isTrue);
    final sentToken = verify(() => repository.enableBiometricLogin(pin: '1234', biometricToken: captureAny(named: 'biometricToken')))
        .captured
        .single as String;
    expect(sentToken.length, greaterThanOrEqualTo(32));

    final credential = await service.readCredential();
    expect(credential?.token, sentToken);
    expect(credential?.documentType, 'CE');
    expect(credential?.documentNumber, '456789');
  });

  test('enable does nothing when the fingerprint dialog is cancelled', () async {
    fingerprintAccepted = false;

    final activated = await service.enable(pin: '1234', documentType: 'CC', documentNumber: '1098765432');

    expect(activated, isFalse);
    verifyNever(() => repository.enableBiometricLogin(pin: any(named: 'pin'), biometricToken: any(named: 'biometricToken')));
    expect(await service.readCredential(), isNull);
  });

  test('the fingerprint key survives logout but not another person signing in on the phone', () async {
    await service.enable(pin: '1234', documentType: 'CC', documentNumber: '1098765432');

    await SecureStorageService().clear(); // lo que hace el cierre de sesión
    expect(await service.readCredential(), isNotNull);

    await service.forgetIfBelongsToAnother(documentType: 'CC', documentNumber: '1098765432');
    expect(await service.readCredential(), isNotNull);

    await service.forgetIfBelongsToAnother(documentType: 'CC', documentNumber: '5555555');
    expect(await service.readCredential(), isNull);
  });

  test('disable turns it off on the server and removes the key from the phone', () async {
    await service.enable(pin: '1234', documentType: 'CC', documentNumber: '1098765432');

    await service.disable();

    verify(() => repository.disableBiometricLogin()).called(1);
    expect(await service.readCredential(), isNull);
  });
}
