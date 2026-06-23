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
}
