import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/models/deposit_request_model.dart';
import 'transfers_provider.dart';

class DepositState {
  DepositState({
    this.amount = 0.0,
    this.method = '',
    this.isLoading = false,
    this.error,
    this.isSuccess = false,
  });

  final double amount;
  final String method;
  final bool isLoading;
  final String? error;
  final bool isSuccess;

  DepositState copyWith({
    double? amount,
    String? method,
    bool? isLoading,
    String? error,
    bool? isSuccess,
  }) {
    return DepositState(
      amount: amount ?? this.amount,
      method: method ?? this.method,
      isLoading: isLoading ?? this.isLoading,
      error: error,
      isSuccess: isSuccess ?? this.isSuccess,
    );
  }
}

class DepositNotifier extends StateNotifier<DepositState> {
  DepositNotifier(this.ref) : super(DepositState());

  final Ref ref;

  void setMethod(String method) {
    state = state.copyWith(method: method, error: null);
  }

  void setAmount(double amount) {
    state = state.copyWith(amount: amount, error: null);
  }

  Future<void> submitDeposit() async {
    if (state.amount <= 0) {
      state = state.copyWith(error: 'El monto debe ser mayor a 0');
      return;
    }
    if (state.method.isEmpty) {
      state = state.copyWith(error: 'Método inválido');
      return;
    }

    state = state.copyWith(isLoading: true, error: null);

    try {
      final repository = ref.read(transfersRepositoryProvider);
      
      await repository.deposit(DepositRequestModel(
        amount: state.amount,
        method: state.method,
        reference: 'APP-DEP-\${DateTime.now().millisecondsSinceEpoch}',
      ));

      // Refresh account balance
      ref.invalidate(myAccountProvider);
      
      // Artificial delay to simulate payment processing
      await Future.delayed(const Duration(seconds: 2));

      state = state.copyWith(isLoading: false, isSuccess: true);
    } on DioException catch (e) {
      final msg = e.response?.data?['message'] ?? 'Error de conexión. Inténtalo de nuevo.';
      state = state.copyWith(isLoading: false, error: msg);
    } catch (e) {
      state = state.copyWith(isLoading: false, error: 'Ocurrió un error inesperado: \$e');
    }
  }

  void reset() {
    state = DepositState();
  }
}

final depositProvider = StateNotifierProvider<DepositNotifier, DepositState>((ref) {
  return DepositNotifier(ref);
});
