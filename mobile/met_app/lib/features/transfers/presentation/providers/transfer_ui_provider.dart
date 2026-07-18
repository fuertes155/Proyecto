import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:dio/dio.dart';
import 'package:uuid/uuid.dart';
import '../../data/models/verify_recipient_model.dart';
import 'transfers_provider.dart';
import '../../data/models/transfer_request_model.dart';

class TransferState {
  TransferState({
    this.step = 0,
    this.recipientIdentifier = '',
    this.recipient,
    this.amount = 0.0,
    this.concept = '',
    this.otp = '',
    this.idempotencyKey = '',
    this.isLoading = false,
    this.error,
  });

  final int step;
  final String recipientIdentifier;
  final VerifyRecipientModel? recipient;
  final double amount;
  final String concept;
  final String otp;
  final String idempotencyKey;
  final bool isLoading;
  final String? error;

  TransferState copyWith({
    int? step,
    String? recipientIdentifier,
    VerifyRecipientModel? recipient,
    double? amount,
    String? concept,
    String? otp,
    String? idempotencyKey,
    bool? isLoading,
    String? error,
  }) {
    return TransferState(
      step: step ?? this.step,
      recipientIdentifier: recipientIdentifier ?? this.recipientIdentifier,
      recipient: recipient ?? this.recipient,
      amount: amount ?? this.amount,
      concept: concept ?? this.concept,
      otp: otp ?? this.otp,
      idempotencyKey: idempotencyKey ?? this.idempotencyKey,
      isLoading: isLoading ?? this.isLoading,
      error: error, // Can be null
    );
  }
}

class TransferNotifier extends StateNotifier<TransferState> {
  TransferNotifier(this.ref) : super(TransferState(idempotencyKey: const Uuid().v4()));

  final Ref ref;

  void nextStep() => state = state.copyWith(step: state.step + 1);
  void previousStep() {
    if (state.step > 0) {
      state = state.copyWith(step: state.step - 1);
    }
  }

  void updateIdentifier(String id) => state = state.copyWith(recipientIdentifier: id, error: null);
  void updateAmount(double amt) => state = state.copyWith(amount: amt, error: null);
  void updateConcept(String conc) => state = state.copyWith(concept: conc);
  void updateOtp(String val) => state = state.copyWith(otp: val);

  Future<bool> verifyRecipient() async {
    if (state.recipientIdentifier.isEmpty) {
      state = state.copyWith(error: 'Ingresa un número válido');
      return false;
    }
    
    state = state.copyWith(isLoading: true, error: null);
    try {
      final repository = ref.read(transfersRepositoryProvider);
      final recipient = await repository.verifyRecipient(state.recipientIdentifier);
      state = state.copyWith(isLoading: false, recipient: recipient);
      nextStep();
      return true;
    } on DioException catch (e) {
      String errorMessage = 'No se encontró el destinatario';
      if (e.response != null && e.response!.data != null) {
        final data = e.response!.data as Map<String, dynamic>;
        if (data.containsKey('message')) {
          errorMessage = data['message'];
        }
      }
      state = state.copyWith(isLoading: false, error: errorMessage);
      return false;
    } catch (e) {
      state = state.copyWith(isLoading: false, error: 'No se encontró el destinatario');
      return false;
    }
  }

  Future<bool> executeTransfer(String pin) async {
    if (state.recipient == null || state.amount <= 0 || pin.isEmpty) return false;
    
    state = state.copyWith(isLoading: true, error: null);
    try {
      final repository = ref.read(transfersRepositoryProvider);
      await repository.executeTransfer(TransferRequestModel(
        destinationAccountId: state.recipient!.accountId,
        amount: state.amount,
        pin: pin,
        idempotencyKey: state.idempotencyKey,
        concept: state.concept,
        otp: state.otp.isNotEmpty ? state.otp : null,
      ));
      
      // Refresh balance
      ref.invalidate(myAccountProvider);
      
      state = state.copyWith(isLoading: false);
      nextStep();
      return true;
    } on DioException catch (e) {
      String errorMessage = 'Ocurrió un error inesperado';
      if (e.response != null && e.response!.data != null) {
        final data = e.response!.data as Map<String, dynamic>;
        if (data.containsKey('message')) {
          errorMessage = data['message'];
        }
      }
      state = state.copyWith(isLoading: false, error: errorMessage);
      return false;
    } catch (e) {
      state = state.copyWith(isLoading: false, error: 'Ocurrió un error inesperado');
      return false;
    }
  }

  Future<void> requestOtp() async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final repository = ref.read(transfersRepositoryProvider);
      await repository.requestTransferOtp();
      state = state.copyWith(isLoading: false);
    } catch (e) {
      state = state.copyWith(isLoading: false, error: 'Error solicitando código OTP');
    }
  }
}

final transferNotifierProvider = StateNotifierProvider.autoDispose<TransferNotifier, TransferState>((ref) {
  return TransferNotifier(ref);
});
