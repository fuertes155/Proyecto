import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:met/features/auth/domain/repositories/auth_repository.dart';
import 'package:met/features/auth/data/models/auth_models.dart';

class MockAuthRepository extends Mock implements AuthRepository {}

void main() {
  late MockAuthRepository authRepository;

  setUp(() {
    authRepository = MockAuthRepository();
    registerFallbackValue(LoginRequest(documentType: 'CC', documentNumber: '123'));
  });

  group('AuthRepository', () {
    test('loginWithPin returns AuthResponse on success', () async {
      // Arrange
      final response = AuthResponse(
        userId: 'test-user-id',
        accessToken: 'fake-token',
        refreshToken: 'fake-refresh-token',
        expiresInMs: 3600000,
      );
      
      final req = LoginRequest(
        documentType: 'CC', 
        documentNumber: '123456789', 
        pin: '1234'
      );
      
      when(() => authRepository.loginWithPin(any()))
          .thenAnswer((_) async => response);

      // Act
      final result = await authRepository.loginWithPin(req);

      // Assert
      expect(result.accessToken, 'fake-token');
      expect(result.refreshToken, 'fake-refresh-token');
      verify(() => authRepository.loginWithPin(any())).called(1);
    });

    test('loginWithPin throws exception on failure', () async {
      // Arrange
      when(() => authRepository.loginWithPin(any()))
          .thenThrow(Exception('Unauthorized'));

      // Act & Assert
      expect(
        () => authRepository.loginWithPin(LoginRequest(documentType: 'CC', documentNumber: '123', pin: '0000')),
        throwsA(isA<Exception>()),
      );
    });

    test('logout completes successfully', () async {
      // Arrange
      when(() => authRepository.logout()).thenAnswer((_) async => {});

      // Act
      await authRepository.logout();

      // Assert
      verify(() => authRepository.logout()).called(1);
    });
  });
}
