import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:local_auth/local_auth.dart';

import '../../../../core/storage/secure_storage_service.dart';
import '../../domain/repositories/auth_repository.dart';
import '../datasources/auth_remote_datasource.dart';
import '../models/auth_models.dart';

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepositoryImpl(
    ref.watch(authRemoteDataSourceProvider),
    ref.watch(secureStorageProvider),
    LocalAuthentication(),
  );
});

class AuthRepositoryImpl implements AuthRepository {
  AuthRepositoryImpl(this._remote, this._storage, this._localAuth);

  final AuthRemoteDataSource _remote;
  final SecureStorageService _storage;
  final LocalAuthentication _localAuth;

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

  @override
  Future<AuthResponse?> loginWithBiometric(LoginRequest request) async {
    final canCheck = await _localAuth.canCheckBiometrics;
    if (!canCheck) return null;

    final authenticated = await _localAuth.authenticate(
      localizedReason: 'Autentícate para acceder a tu cuenta',
      options: const AuthenticationOptions(biometricOnly: true),
    );
    if (!authenticated) return null;

    final response = await _remote.login(request);
    await _storage.saveTokens(
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      userId: response.userId,
    );
    return response;
  }

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
