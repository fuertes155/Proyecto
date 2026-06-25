import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/scheduled_savings_remote_datasource.dart';
import '../../data/models/scheduled_savings_models.dart';

final scheduledSavingsListProvider =
    StateNotifierProvider<ScheduledSavingsListNotifier, AsyncValue<List<ScheduledSavingsAccount>>>((ref) {
  return ScheduledSavingsListNotifier(ref.watch(scheduledSavingsRemoteDataSourceProvider));
});

class ScheduledSavingsListNotifier extends StateNotifier<AsyncValue<List<ScheduledSavingsAccount>>> {
  ScheduledSavingsListNotifier(this._dataSource) : super(const AsyncValue.loading());

  final ScheduledSavingsRemoteDataSource _dataSource;

  Future<void> load() async {
    state = const AsyncValue.loading();
    try {
      final accounts = await _dataSource.listAccounts();
      state = AsyncValue.data(accounts);
    } catch (error, stackTrace) {
      state = AsyncValue.error(error, stackTrace);
    }
  }

  Future<void> create(CreateScheduledSavingsRequest request) async {
    await _dataSource.createAccount(request);
    await load();
  }

  Future<void> pauseAccount(String accountId) async {
    await _dataSource.updateAccount(accountId, UpdateScheduledSavingsRequest(status: 'PAUSED'));
    await load();
  }

  Future<void> resumeAccount(String accountId) async {
    await _dataSource.updateAccount(accountId, UpdateScheduledSavingsRequest(status: 'ACTIVE'));
    await load();
  }

  Future<void> cancelAccount(String accountId) async {
    await _dataSource.updateAccount(accountId, UpdateScheduledSavingsRequest(status: 'CANCELLED'));
    await load();
  }

  Future<void> withdrawAccount(String accountId, double amount, String type) async {
    await _dataSource.withdraw(accountId, WithdrawSavingsRequest(amount: amount, withdrawalType: type));
    await load();
  }
}

final scheduledSavingsDetailProvider = FutureProvider.family<ScheduledSavingsAccount, String>((ref, accountId) {
  return ref.watch(scheduledSavingsRemoteDataSourceProvider).getAccount(accountId);
});

final contributionHistoryProvider = FutureProvider.family<List<ContributionItem>, String>((ref, accountId) {
  return ref.watch(scheduledSavingsRemoteDataSourceProvider).getContributions(accountId);
});
