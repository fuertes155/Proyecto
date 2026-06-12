import 'package:flutter_riverpod/flutter_riverpod.dart';

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

final loanApplicationsProvider = FutureProvider<List<LoanApplication>>((ref) {
  return ref.watch(loanRemoteDataSourceProvider).listApplications();
});

final loanApplicationDetailProvider = FutureProvider.family<LoanApplication, String>((ref, id) {
  return ref.watch(loanRemoteDataSourceProvider).getApplication(id);
});
