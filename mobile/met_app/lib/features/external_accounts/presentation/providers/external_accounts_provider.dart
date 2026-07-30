import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/external_accounts_remote_datasource.dart';
import '../../data/models/bank_model.dart';
import '../../data/models/external_bank_account_model.dart';
import '../../data/models/register_external_bank_account_request.dart';

/// [type]: 'PAYOUT' (para el selector de banco al registrar una cuenta
/// externa) o 'PSE' (para el futuro selector nativo de depósitos).
final banksProvider = FutureProvider.family<List<BankModel>, String>((ref, type) async {
  final dataSource = ref.watch(externalAccountsRemoteDataSourceProvider);
  return dataSource.listBanks(type);
});

final externalBankAccountsListProvider =
    StateNotifierProvider<ExternalBankAccountsNotifier, AsyncValue<List<ExternalBankAccountModel>>>((ref) {
  return ExternalBankAccountsNotifier(ref.watch(externalAccountsRemoteDataSourceProvider));
});

class ExternalBankAccountsNotifier extends StateNotifier<AsyncValue<List<ExternalBankAccountModel>>> {
  ExternalBankAccountsNotifier(this._dataSource) : super(const AsyncValue.loading());

  final ExternalAccountsRemoteDataSource _dataSource;

  Future<void> load() async {
    state = const AsyncValue.loading();
    try {
      final accounts = await _dataSource.listMyAccounts();
      state = AsyncValue.data(accounts);
    } catch (error, stackTrace) {
      state = AsyncValue.error(error, stackTrace);
    }
  }

  Future<void> register(RegisterExternalBankAccountRequest request) async {
    await _dataSource.registerAccount(request);
    await load();
  }

  Future<void> resendVerification(String accountId) async {
    await _dataSource.resendVerification(accountId);
    await load();
  }

  Future<void> confirmVerification(String accountId, int amount) async {
    await _dataSource.confirmVerification(accountId, amount);
    await load();
  }
}
