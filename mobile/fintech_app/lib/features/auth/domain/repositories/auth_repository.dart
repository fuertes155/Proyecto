import '../../data/models/auth_models.dart';

abstract class AuthRepository {
  Future<UserResponse> register(RegisterRequest request);

  Future<AuthResponse> loginWithPin(LoginRequest request);

  Future<AuthResponse?> loginWithBiometric(LoginRequest request);

  Future<UserResponse> getProfile();

  Future<void> logout();

  Future<bool> hasSession();
}
