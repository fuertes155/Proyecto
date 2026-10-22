import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../auth/presentation/providers/auth_provider.dart';
import '../../data/datasources/loan_remote_datasource.dart';
import '../../data/models/loan_models.dart';

final loanSimulationProvider =
    StateNotifierProvider<LoanSimulationNotifier, AsyncValue<LoanSimulationResult?>>((ref) {
  return LoanSimulationNotifier(ref.watch(loanRemoteDataSourceProvider));
});

class LoanSimulationNotifier extends StateNotifier<AsyncValue<LoanSimulationResult?>> {
  LoanSimulationNotifier(this._dataSource) : super(const AsyncValue.data(null));

  final LoanRemoteDataSource _dataSource;

  Future<void> simulate(SimulateLoanRequest request) async {
    state = const AsyncValue.loading();
    try {
      state = AsyncValue.data(await _dataSource.simulate(request));
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  void clear() => state = const AsyncValue.data(null);
}

// autoDispose: cada visita al simulador parte de cero. Si el resultado sobreviviera a la pantalla,
// al volver se mostraría la consulta anterior (quizá de otro socio) antes de autorizar la nueva, y
// con límites menores que los valores iniciales el simulador fallaba.
final loanEligibilityProvider =
    StateNotifierProvider.autoDispose<LoanEligibilityNotifier, AsyncValue<LoanEligibility?>>((ref) {
  return LoanEligibilityNotifier(ref.watch(loanRemoteDataSourceProvider));
});

class LoanEligibilityNotifier extends StateNotifier<AsyncValue<LoanEligibility?>> {
  LoanEligibilityNotifier(this._dataSource) : super(const AsyncValue.data(null));

  final LoanRemoteDataSource _dataSource;

  Future<void> fetch() async {
    state = const AsyncValue.loading();
    try {
      state = AsyncValue.data(
        await _dataSource.getEligibility(LoanEligibilityRequest(acceptedHabeasData: true)),
      );
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  void clear() => state = const AsyncValue.data(null);
}

final loanApplicationsProvider = FutureProvider<List<LoanApplication>>((ref) {
  ref.watch(sessionUserIdProvider); // se descarta al cambiar de socio
  return ref.watch(loanRemoteDataSourceProvider).listApplications();
});

final loanApplicationDetailProvider = FutureProvider.family<LoanApplication, String>((ref, id) {
  ref.watch(sessionUserIdProvider); // se descarta al cambiar de socio
  return ref.watch(loanRemoteDataSourceProvider).getApplication(id);
});
