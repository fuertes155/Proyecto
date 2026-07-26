import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client_provider.dart';
import '../../domain/entities/home_summary.dart';
import '../../domain/repositories/home_repository.dart';
import '../../data/repositories/home_remote_repository.dart';

/// Provider para inyectar el repositorio
final homeRepositoryProvider = Provider<HomeRepository>((ref) {
  final dio = ref.watch(apiClientProvider);
  return HomeRemoteRepository(dio);
});

/// Notifier que maneja el estado del home
class HomeNotifier extends AsyncNotifier<HomeSummary> {
  @override
  Future<HomeSummary> build() async {
    return _fetchSummary();
  }

  Future<HomeSummary> _fetchSummary() async {
    final repo = ref.read(homeRepositoryProvider);
    return repo.getHomeSummary();
  }

  /// Recarga los datos (ej. para pull-to-refresh)
  Future<void> refresh() async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() => _fetchSummary());
  }
}

/// Provider para consumir el estado del home
final homeProvider = AsyncNotifierProvider<HomeNotifier, HomeSummary>(
  () => HomeNotifier(),
);
