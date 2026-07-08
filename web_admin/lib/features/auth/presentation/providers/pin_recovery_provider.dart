import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/models/auth_models.dart';
import '../../data/repositories/auth_repository_impl.dart';
import 'auth_provider.dart';

final pinRecoveryProvider = StateNotifierProvider<PinRecoveryNotifier, PinRecoveryState>((ref) {
  final authRepo = ref.watch(authRepositoryProvider);
  return PinRecoveryNotifier(authRepo);
});

class PinRecoveryState {
  PinRecoveryState({
    this.currentStep = 0,
    this.isLoading = false,
    this.error,
    this.documentType = 'CC',
    this.documentNumber = '',
    this.otpCode = '',
    this.newPin = '',
  });

  final int currentStep;
  final bool isLoading;
  final String? error;
  final String documentType;
  final String documentNumber;
  final String otpCode;
  final String newPin;

  PinRecoveryState copyWith({
    int? currentStep,
    bool? isLoading,
    String? error,
    String? documentType,
    String? documentNumber,
    String? otpCode,
    String? newPin,
  }) {
    return PinRecoveryState(
      currentStep: currentStep ?? this.currentStep,
      isLoading: isLoading ?? this.isLoading,
      error: error,
      documentType: documentType ?? this.documentType,
      documentNumber: documentNumber ?? this.documentNumber,
      otpCode: otpCode ?? this.otpCode,
      newPin: newPin ?? this.newPin,
    );
  }
}

class PinRecoveryNotifier extends StateNotifier<PinRecoveryState> {
  PinRecoveryNotifier(this._authRepo) : super(PinRecoveryState());

  final _authRepo;

  void nextStep() {
    if (state.currentStep < 2) {
      state = state.copyWith(currentStep: state.currentStep + 1, error: null);
    }
  }

  void previousStep() {
    if (state.currentStep > 0) {
      state = state.copyWith(currentStep: state.currentStep - 1, error: null);
    }
  }

  Future<bool> requestRecovery(String documentType, String documentNumber) async {
    state = state.copyWith(isLoading: true, error: null, documentType: documentType, documentNumber: documentNumber);
    try {
      final request = PinRecoveryRequest(documentType: documentType, documentNumber: documentNumber);
      await _authRepo.requestPinRecovery(request);
      state = state.copyWith(isLoading: false);
      nextStep();
      return true;
    } on DioException catch (e) {
      String errorMessage = 'No se encontró el usuario';
      if (e.response != null && e.response!.data != null) {
        final data = e.response!.data as Map<String, dynamic>;
        if (data.containsKey('message')) {
          errorMessage = data['message'];
        }
      }
      state = state.copyWith(isLoading: false, error: errorMessage);
      return false;
    } catch (e) {
      state = state.copyWith(isLoading: false, error: 'Error inesperado al solicitar recuperación');
      return false;
    }
  }

  Future<bool> resetPin(String otpCode, String newPin) async {
    state = state.copyWith(isLoading: true, error: null, otpCode: otpCode, newPin: newPin);
    try {
      final request = PinRecoveryResetRequest(
        documentType: state.documentType,
        documentNumber: state.documentNumber,
        otpCode: otpCode,
        newPin: newPin,
      );
      await _authRepo.resetPinWithOtp(request);
      state = state.copyWith(isLoading: false);
      return true; // Success!
    } on DioException catch (e) {
      String errorMessage = 'El código OTP es incorrecto o expiró';
      if (e.response != null && e.response!.data != null) {
        final data = e.response!.data as Map<String, dynamic>;
        if (data.containsKey('message')) {
          errorMessage = data['message'];
        }
      }
      state = state.copyWith(isLoading: false, error: errorMessage);
      return false;
    } catch (e) {
      state = state.copyWith(isLoading: false, error: 'Error inesperado al cambiar el PIN');
      return false;
    }
  }
}
