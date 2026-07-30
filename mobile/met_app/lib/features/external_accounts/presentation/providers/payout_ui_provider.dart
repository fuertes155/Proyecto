import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:dio/dio.dart';
import 'package:uuid/uuid.dart';

import '../../../transfers/presentation/providers/transfers_provider.dart';
import '../../data/datasources/external_accounts_remote_datasource.dart';
import '../../data/models/external_bank_account_model.dart';
import '../../data/models/payout_request_model.dart';

class PayoutState {
  PayoutState({
    this.step = 0,
    this.selectedAccount,
    this.amount = 0.0,
    this.concept = '',
    this.otp = '',
    this.idempotencyKey = '',
    this.isLoading = false,
    this.error,
  });

  final int step;
  final ExternalBankAccountModel? selectedAccount;
  final double amount;
  final String concept;
  final String otp;
  final String idempotencyKey;
  final bool isLoading;
  final String? error;

  PayoutState copyWith({
    int? step,
    ExternalBankAccountModel? selectedAccount,
    double? amount,
    String? concept,
    String? otp,
    String? idempotencyKey,
    bool? isLoading,
    String? error,
  }) {
    return PayoutState(
      step: step ?? this.step,
      selectedAccount: selectedAccount ?? this.selectedAccount,
      amount: amount ?? this.amount,
      concept: concept ?? this.concept,
      otp: otp ?? this.otp,
      idempotencyKey: idempotencyKey ?? this.idempotencyKey,
      isLoading: isLoading ?? this.isLoading,
      error: error, // Puede ser null explícitamente
    );
  }
}

class PayoutNotifier extends StateNotifier<PayoutState> {
  PayoutNotifier(this.ref) : super(PayoutState(idempotencyKey: const Uuid().v4()));

  final Ref ref;

  void nextStep() => state = state.copyWith(step: state.step + 1);
  void previousStep() {
    if (state.step > 0) {
      state = state.copyWith(step: state.step - 1);
    }
  }

  void selectAccount(ExternalBankAccountModel account) {
    state = state.copyWith(selectedAccount: account, error: null);
    nextStep();
  }

  void updateAmount(double amt) => state = state.copyWith(amount: amt, error: null);
  void updateConcept(String conc) => state = state.copyWith(concept: conc);
  void updateOtp(String val) => state = state.copyWith(otp: val);

  Future<bool> executePayout(String pin) async {
    if (state.selectedAccount == null || state.amount <= 0 || pin.isEmpty) return false;

    state = state.copyWith(isLoading: true, error: null);
    try {
      final dataSource = ref.read(externalAccountsRemoteDataSourceProvider);
      await dataSource.executePayout(PayoutRequestModel(
        externalBankAccountId: state.selectedAccount!.id,
        amount: state.amount,
        pin: pin,
        idempotencyKey: state.idempotencyKey,
        concept: state.concept,
        otp: state.otp.isNotEmpty ? state.otp : null,
      ));

      // Refresca el saldo de la wallet (ya se debitó en el backend)
      ref.invalidate(myAccountProvider);

      state = state.copyWith(isLoading: false);
      nextStep();
      return true;
    } on DioException catch (e) {
      String errorMessage = 'Ocurrió un error inesperado';
      if (e.response != null && e.response!.data != null && e.response!.data is Map) {
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
      final dataSource = ref.read(externalAccountsRemoteDataSourceProvider);
      await dataSource.requestPayoutOtp();
      state = state.copyWith(isLoading: false);
    } catch (e) {
      state = state.copyWith(isLoading: false, error: 'Error solicitando código OTP');
    }
  }
}

final payoutNotifierProvider = StateNotifierProvider.autoDispose<PayoutNotifier, PayoutState>((ref) {
  return PayoutNotifier(ref);
});
