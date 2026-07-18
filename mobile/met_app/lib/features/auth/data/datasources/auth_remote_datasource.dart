import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client_provider.dart';
import '../../../../core/security/rsa_encryption_service.dart';
import '../models/auth_models.dart';

final authRemoteDataSourceProvider = Provider<AuthRemoteDataSource>((ref) {
  return AuthRemoteDataSource(
    ref.watch(apiClientProvider),
    ref.watch(rsaEncryptionServiceProvider),
  );
});

class AuthRemoteDataSource {
  AuthRemoteDataSource(this._dio, this._rsaEncryptionService);

  final Dio _dio;
  final RsaEncryptionService _rsaEncryptionService;

  Future<UserResponse> register(RegisterRequest request) async {
    final payload = request.toJson();
    if (payload['pin'] != null) {
      payload['pin'] = await _rsaEncryptionService.encryptPin(payload['pin']);
    }
    final response =
        await _dio.post('/v1/auth/register', data: payload);
    return UserResponse.fromJson(response.data as Map<String, dynamic>);
  }

  Future<AuthResponse> login(LoginRequest request) async {
    final payload = request.toJson();
    if (payload['pin'] != null) {
      payload['pin'] = await _rsaEncryptionService.encryptPin(payload['pin'] as String);
    }
    final response = await _dio.post('/v1/auth/login', data: payload);
    return AuthResponse.fromJson(response.data as Map<String, dynamic>);
  }

  Future<UserResponse> getProfile() async {
    final response = await _dio.get('/v1/auth/me');
    return UserResponse.fromJson(response.data as Map<String, dynamic>);
  }

  Future<UserResponse> updateProfile({required String email, required String phone}) async {
    final response = await _dio.put('/v1/auth/me', data: {
      'email': email,
      'phone': phone,
    });
    return UserResponse.fromJson(response.data as Map<String, dynamic>);
  }

  Future<void> updatePin({required String currentPin, required String newPin}) async {
    final encryptedCurrent = await _rsaEncryptionService.encryptPin(currentPin);
    final encryptedNew = await _rsaEncryptionService.encryptPin(newPin);
    await _dio.put('/v1/auth/pin', data: {
      'currentPin': encryptedCurrent,
      'newPin': encryptedNew,
    });
  }

  Future<UserResponse> updateNotifications({required bool emailEnabled, required bool pushEnabled}) async {
    final response = await _dio.put('/v1/auth/notifications', data: {
      'emailNotificationsEnabled': emailEnabled,
      'pushNotificationsEnabled': pushEnabled,
    });
    return UserResponse.fromJson(response.data as Map<String, dynamic>);
  }

  Future<void> requestPinRecovery(PinRecoveryRequest request) async {
    await _dio.post('/v1/auth/pin-recovery/request', data: request.toJson());
  }

  Future<void> resetPinWithOtp(PinRecoveryResetRequest request) async {
    final payload = request.toJson();
    if (payload['newPin'] != null) {
      payload['newPin'] = await _rsaEncryptionService.encryptPin(payload['newPin']);
    }
    await _dio.post('/v1/auth/pin-recovery/reset', data: payload);
  }

  Future<void> logout() async {
    try {
      await _dio.post('/v1/auth/logout');
    } catch (e) {
      // Ignoramos errores de red en logout, el token local se limpiará igual
    }
  }
}
