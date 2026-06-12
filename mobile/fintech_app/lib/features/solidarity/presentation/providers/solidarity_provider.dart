import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/solidarity_remote_datasource.dart';
import '../../data/models/solidarity_models.dart';

final solidarityGroupsProvider =
    StateNotifierProvider<SolidarityGroupsNotifier, AsyncValue<List<SolidarityGroup>>>((ref) {
  return SolidarityGroupsNotifier(ref.watch(solidarityRemoteDataSourceProvider));
});

class SolidarityGroupsNotifier extends StateNotifier<AsyncValue<List<SolidarityGroup>>> {
  SolidarityGroupsNotifier(this._dataSource) : super(const AsyncValue.loading());

  final SolidarityRemoteDataSource _dataSource;

  Future<void> load() async {
    state = const AsyncValue.loading();
    try {
      state = AsyncValue.data(await _dataSource.listGroups());
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<SolidarityGroup> create(CreateSolidarityGroupRequest request) async {
    final group = await _dataSource.createGroup(request);
    await load();
    return group;
  }

  Future<void> join(JoinSolidarityGroupRequest request) async {
    await _dataSource.joinGroup(request);
    await load();
  }
}

final solidarityGroupDetailProvider = FutureProvider.family<SolidarityGroup, String>((ref, groupId) {
  return ref.watch(solidarityRemoteDataSourceProvider).getGroup(groupId);
});

final solidarityLoansProvider = FutureProvider.family<List<MicroLoan>, String>((ref, groupId) {
  return ref.watch(solidarityRemoteDataSourceProvider).listLoans(groupId);
});

final solidarityTransactionsProvider = FutureProvider.family<List<PoolTransaction>, String>((ref, groupId) {
  return ref.watch(solidarityRemoteDataSourceProvider).listTransactions(groupId);
});
