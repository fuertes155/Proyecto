import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/storage/secure_storage_service.dart';
import '../../domain/repositories/auth_repository.dart';
import '../datasources/auth_remote_datasource.dart';
import '../models/auth_models.dart';

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepositoryImpl(
    ref.watch(authRemoteDataSourceProvider),
    ref.watch(secureStorageProvider),
  );
});

class AuthRepositoryImpl implements AuthRepository {
  AuthRepositoryImpl(this._remote, this._storage);

  final AuthRemoteDataSource _remote;
  final SecureStorageService _storage;

  @override
  Future<UserResponse> register(RegisterRequest request) {
    return _remote.register(request);
  }

  @override
  Future<AuthResponse> loginWithPin(LoginRequest request) async {
    final response = await _remote.login(request);
    await _storage.saveTokens(
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      userId: response.userId,
    );
    return response;
  }

  /// La huella ya se confirmó en el celular antes de llegar aquí (ver BiometricLoginService):
  /// pedirla de nuevo mostraría el diálogo del sistema dos veces seguidas.
  @override
  Future<AuthResponse> loginWithBiometric(LoginRequest request) async {
    final response = await _remote.login(request);
    await _storage.saveTokens(
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      userId: response.userId,
    );
    return response;
  }

  @override
  Future<void> enableBiometricLogin({required String pin, required String biometricToken}) =>
      _remote.enableBiometricLogin(pin: pin, biometricToken: biometricToken);

  @override
  Future<void> disableBiometricLogin() => _remote.disableBiometricLogin();

  @override
  Future<UserResponse> getProfile() => _remote.getProfile();

  @override
  Future<void> logout() async {
    await _remote.logout();
    await _storage.clear();
  }

  @override
  Future<bool> hasSession() async {
    final token = await _storage.readAccessToken();
    return token != null && token.isNotEmpty;
  }

  @override
  Future<UserResponse> updateProfile({required String email, required String phone}) {
    return _remote.updateProfile(email: email, phone: phone);
  }

  @override
  Future<void> updatePin({required String currentPin, required String newPin}) {
    return _remote.updatePin(currentPin: currentPin, newPin: newPin);
  }

  @override
  Future<UserResponse> updateNotifications({required bool emailEnabled, required bool pushEnabled}) {
    return _remote.updateNotifications(emailEnabled: emailEnabled, pushEnabled: pushEnabled);
  }

  @override
  Future<void> requestPinRecovery(PinRecoveryRequest request) {
    return _remote.requestPinRecovery(request);
  }

  @override
  Future<void> resetPinWithOtp(PinRecoveryResetRequest request) {
    return _remote.resetPinWithOtp(request);
  }
}
