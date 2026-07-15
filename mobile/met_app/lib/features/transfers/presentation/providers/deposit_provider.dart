import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'transfers_provider.dart';

/// Estado del flujo de depósito.
class DepositState {
  DepositState({
    this.amount = 0.0,
    this.method = '',
    this.isLoading = false,
    this.error,
    this.isSuccess = false,
    this.paymentUrl,
  });

  final double amount;
  final String method;
  final bool isLoading;
  final String? error;
  final bool isSuccess;

  /// URL del checkout de Wompi — cuando se establece, la UI navega al WebView.
  final String? paymentUrl;

  DepositState copyWith({
    double? amount,
    String? method,
    bool? isLoading,
    String? error,
    bool? isSuccess,
    String? paymentUrl,
    bool clearPaymentUrl = false,
  }) {
    return DepositState(
      amount: amount ?? this.amount,
      method: method ?? this.method,
      isLoading: isLoading ?? this.isLoading,
      error: error,
      isSuccess: isSuccess ?? this.isSuccess,
      paymentUrl: clearPaymentUrl ? null : (paymentUrl ?? this.paymentUrl),
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

  /// Inicia el depósito para cualquier método de pago soportado por Wompi.
  ///
  /// Todos los métodos (Nequi, PSE, Bre-B, tarjetas) pasan por el checkout
  /// de Wompi — el usuario elige el método dentro de la pasarela.
  ///
  /// El parámetro [method] se usa solo para el concepto de la transacción.
  Future<void> submitDeposit(String method) async {
    if (state.amount <= 0) {
      state = state.copyWith(error: 'El monto debe ser mayor a 0');
      return;
    }

    state = state.copyWith(isLoading: true, error: null, clearPaymentUrl: true);

    try {
      final repository = ref.read(transfersRepositoryProvider);

      // Todos los métodos de depósito generan un link de Wompi Checkout
      final url = await repository.generatePseLink(
        state.amount,
        'metapp://deposit/success',
      );

      state = state.copyWith(isLoading: false, paymentUrl: url);
    } on DioException catch (e) {
      final msg = e.response?.data?['message'] ?? 'Error de conexión. Inténtalo de nuevo.';
      state = state.copyWith(isLoading: false, error: msg);
    } catch (e) {
      state = state.copyWith(isLoading: false, error: 'Ocurrió un error inesperado: $e');
    }
  }

  /// Marca el depósito como exitoso (llamado desde el WebView al confirmar el pago).
  void markSuccess() {
    state = state.copyWith(isLoading: false, isSuccess: true, clearPaymentUrl: true);
    ref.invalidate(myAccountProvider);
  }

  void reset() {
    state = DepositState();
  }
}

final depositProvider = StateNotifierProvider<DepositNotifier, DepositState>((ref) {
  return DepositNotifier(ref);
});
