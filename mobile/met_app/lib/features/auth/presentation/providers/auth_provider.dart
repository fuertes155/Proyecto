import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/auth_models.dart';
import '../../data/repositories/auth_repository_impl.dart';
import '../../domain/repositories/auth_repository.dart';

final authStateProvider = StateNotifierProvider<AuthNotifier, AsyncValue<UserResponse?>>((ref) {
  return AuthNotifier(ref.watch(authRepositoryProvider));
});

class AuthNotifier extends StateNotifier<AsyncValue<UserResponse?>> {
  AuthNotifier(this._repository) : super(const AsyncValue.data(null));

  final AuthRepository _repository;

  Future<void> checkSession() async {
    state = const AsyncValue.loading();
    try {
      final hasSession = await _repository.hasSession();
      if (!hasSession) {
        state = const AsyncValue.data(null);
        return;
      }
      final profile = await _repository.getProfile();
      state = AsyncValue.data(profile);
    } catch (error, stackTrace) {
      state = AsyncValue.error(error, stackTrace);
    }
  }

  Future<void> loginWithPin(LoginRequest request) async {
    state = const AsyncValue.loading();
    try {
      await _repository.loginWithPin(request);
      final profile = await _repository.getProfile();
      state = AsyncValue.data(profile);
    } catch (error, stackTrace) {
      state = AsyncValue.error(error, stackTrace);
    }
  }

  Future<void> loginWithBiometric(LoginRequest request) async {
    state = const AsyncValue.loading();
    try {
      final response = await _repository.loginWithBiometric(request);
      if (response == null) {
        state = AsyncValue.error('Autenticación biométrica cancelada', StackTrace.current);
        return;
      }
      final profile = await _repository.getProfile();
      state = AsyncValue.data(profile);
    } catch (error, stackTrace) {
      state = AsyncValue.error(error, stackTrace);
    }
  }

  Future<void> logout() async {
    await _repository.logout();
    state = const AsyncValue.data(null);
  }

  Future<void> updateProfile({required String email, required String phone}) async {
    try {
      final updatedUser = await _repository.updateProfile(email: email, phone: phone);
      state = AsyncValue.data(updatedUser);
    } catch (error) {
      throw error;
    }
  }

  Future<void> updatePin({required String currentPin, required String newPin}) async {
    try {
      await _repository.updatePin(currentPin: currentPin, newPin: newPin);
    } catch (error) {
      throw error;
    }
  }

  Future<void> updateNotifications({required bool emailEnabled, required bool pushEnabled}) async {
    try {
      final updatedUser = await _repository.updateNotifications(emailEnabled: emailEnabled, pushEnabled: pushEnabled);
      state = AsyncValue.data(updatedUser);
    } catch (error) {
      throw error;
    }
  }
}
