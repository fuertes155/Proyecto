import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'transfers_provider.dart';
import '../../../home/presentation/providers/home_provider.dart';

/// Estado del flujo de depósito.
class DepositState {
  DepositState({
    this.amount = 0.0,
    this.method = '',
    this.bankCode,
    this.isLoading = false,
    this.error,
    this.isSuccess = false,
    this.paymentUrl,
    this.baselineBalance,
  });

  final double amount;
  final String method;

  /// Banco elegido en el selector nativo de PSE (null para el resto de métodos).
  final String? bankCode;
  final bool isLoading;
  final String? error;
  final bool isSuccess;

  /// URL del checkout de Wompi — cuando se establece, la UI navega al WebView.
  final String? paymentUrl;

  /// Saldo de la cuenta justo antes de abrir la pasarela de pago. La pantalla
  /// de espera compara el saldo actual contra este valor para detectar,
  /// sin atajos client-side, que el webhook real de Wompi ya acreditó el depósito.
  final double? baselineBalance;

  DepositState copyWith({
    double? amount,
    String? method,
    String? bankCode,
    bool? isLoading,
    String? error,
    bool? isSuccess,
    String? paymentUrl,
    bool clearPaymentUrl = false,
    double? baselineBalance,
  }) {
    return DepositState(
      amount: amount ?? this.amount,
      method: method ?? this.method,
      bankCode: bankCode ?? this.bankCode,
      isLoading: isLoading ?? this.isLoading,
      error: error,
      isSuccess: isSuccess ?? this.isSuccess,
      paymentUrl: clearPaymentUrl ? null : (paymentUrl ?? this.paymentUrl),
      baselineBalance: baselineBalance ?? this.baselineBalance,
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

  void setBankCode(String bankCode) {
    state = state.copyWith(bankCode: bankCode, error: null);
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
      final currentAccount = await repository.getMyAccount();

      // Todos los métodos de depósito generan un link de Wompi Checkout
      final url = await repository.generatePseLink(
        state.amount,
        'metapp://deposit/success',
      );

      state = state.copyWith(
        isLoading: false,
        paymentUrl: url,
        baselineBalance: currentAccount.principalBalance,
      );
    } on DioException catch (e) {
      final msg = e.response?.data?['message'] ?? 'Error de conexión. Inténtalo de nuevo.';
      state = state.copyWith(isLoading: false, error: msg);
    } catch (e) {
      state = state.copyWith(isLoading: false, error: 'Ocurrió un error inesperado: $e');
    }
  }

  /// Inicia un depósito PSE nativo: el banco ya fue elegido dentro de la app
  /// (ver [setBankCode]) en vez de dentro del checkout hosteado de Wompi.
  Future<void> submitNativePseDeposit() async {
    if (state.amount <= 0) {
      state = state.copyWith(error: 'El monto debe ser mayor a 0');
      return;
    }
    if (state.bankCode == null || state.bankCode!.isEmpty) {
      state = state.copyWith(error: 'Selecciona tu banco para continuar');
      return;
    }

    state = state.copyWith(isLoading: true, error: null, clearPaymentUrl: true);

    try {
      final repository = ref.read(transfersRepositoryProvider);
      final currentAccount = await repository.getMyAccount();
      final url = await repository.createNativePseDeposit(
        state.amount,
        state.bankCode!,
        'metapp://deposit/success',
      );
      state = state.copyWith(
        isLoading: false,
        paymentUrl: url,
        baselineBalance: currentAccount.principalBalance,
      );
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
    // La pantalla de inicio (saldo, capital bloqueado, intereses) usa homeProvider,
    // no myAccountProvider — sin esto, un depósito exitoso no se reflejaba ahí hasta
    // hacer pull-to-refresh manual.
    ref.invalidate(homeProvider);
  }

  void reset() {
    state = DepositState();
  }
}

final depositProvider = StateNotifierProvider<DepositNotifier, DepositState>((ref) {
  return DepositNotifier(ref);
});
