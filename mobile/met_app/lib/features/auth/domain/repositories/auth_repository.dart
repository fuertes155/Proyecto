import '../../data/models/auth_models.dart';

abstract class AuthRepository {
  Future<UserResponse> register(RegisterRequest request);

  Future<AuthResponse> loginWithPin(LoginRequest request);

  Future<AuthResponse> loginWithBiometric(LoginRequest request);

  /// Activa el ingreso con huella: el servidor confirma el [pin] y guarda el hash de [biometricToken].
  Future<void> enableBiometricLogin({required String pin, required String biometricToken});

  Future<void> disableBiometricLogin();

  Future<UserResponse> getProfile();

  Future<void> logout();

  Future<bool> hasSession();
  
  Future<UserResponse> updateProfile({required String email, required String phone});
  
  Future<void> updatePin({required String currentPin, required String newPin});
  
  Future<UserResponse> updateNotifications({required bool emailEnabled, required bool pushEnabled});

  Future<void> requestPinRecovery(PinRecoveryRequest request);

  Future<void> resetPinWithOtp(PinRecoveryResetRequest request);
}
