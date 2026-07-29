import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/network/api_client_provider.dart';
import '../../data/datasources/transfers_remote_datasource.dart';
import '../../data/models/core_account_model.dart';
import '../../data/models/recent_recipient_model.dart';
import '../../data/repositories/transfers_repository_impl.dart';



final transfersRepositoryProvider = Provider<TransfersRepositoryImpl>((ref) {
  final remoteDataSource = ref.watch(transfersRemoteDataSourceProvider);
  return TransfersRepositoryImpl(remoteDataSource);
});

final myAccountProvider = FutureProvider<CoreAccountModel>((ref) async {
  final repository = ref.watch(transfersRepositoryProvider);
  return repository.getMyAccount();
});

final recentRecipientsProvider = FutureProvider.autoDispose<List<RecentRecipientModel>>((ref) async {
  final repository = ref.watch(transfersRepositoryProvider);
  return repository.getRecentRecipients();
});
