import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client_provider.dart';
import '../models/auth_models.dart';

final authRemoteDataSourceProvider = Provider<AuthRemoteDataSource>((ref) {
  return AuthRemoteDataSource(ref.watch(apiClientProvider));
});

class AuthRemoteDataSource {
  AuthRemoteDataSource(this._dio);

  final Dio _dio;

  Future<UserResponse> register(RegisterRequest request) async {
    final response =
        await _dio.post('/v1/auth/register', data: request.toJson());
    return UserResponse.fromJson(response.data as Map<String, dynamic>);
  }

  Future<AuthResponse> login(LoginRequest request) async {
    final response = await _dio.post('/v1/auth/login', data: request.toJson());
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
    await _dio.put('/v1/auth/pin', data: {
      'currentPin': currentPin,
      'newPin': newPin,
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
    await _dio.post('/v1/auth/pin-recovery/reset', data: request.toJson());
  }

  Future<void> logout() async {
    try {
      await _dio.post('/v1/auth/logout');
    } catch (e) {
      // Ignoramos errores de red en logout, el token local se limpiará igual
    }
  }
}
