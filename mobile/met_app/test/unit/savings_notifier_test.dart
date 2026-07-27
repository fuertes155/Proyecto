import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:met/features/savings/presentation/providers/scheduled_savings_provider.dart';
import 'package:met/features/savings/data/datasources/scheduled_savings_remote_datasource.dart';
import 'package:met/features/savings/data/models/scheduled_savings_models.dart';

class MockScheduledSavingsRemoteDataSource extends Mock implements ScheduledSavingsRemoteDataSource {}

class FakeCreateScheduledSavingsRequest extends Fake implements CreateScheduledSavingsRequest {}
class FakeUpdateScheduledSavingsRequest extends Fake implements UpdateScheduledSavingsRequest {}

void main() {
  late MockScheduledSavingsRemoteDataSource dataSource;
  late ScheduledSavingsListNotifier notifier;

  setUpAll(() {
    registerFallbackValue(FakeCreateScheduledSavingsRequest());
    registerFallbackValue(FakeUpdateScheduledSavingsRequest());
  });

  setUp(() {
    dataSource = MockScheduledSavingsRemoteDataSource();
    notifier = ScheduledSavingsListNotifier(dataSource);
  });

  group('ScheduledSavingsListNotifier', () {
    test('initial state is loading', () {
      expect(notifier.state, const AsyncValue<List<ScheduledSavingsAccount>>.loading());
    });

    test('load sets state to data on success', () async {
      final mockAccounts = [
        ScheduledSavingsAccount(
          id: 'acc-1',
          name: 'Vacaciones',
          status: 'ACTIVE',
          frequency: 'MONTHLY',
          contributionAmount: 1000,
          currentBalance: 5000,
          nextContributionDate: DateTime.now().toIso8601String(),
          progressPercentage: 50.0,
        )
      ];

      when(() => dataSource.listAccounts()).thenAnswer((_) async => mockAccounts);

      await notifier.load();

      expect(notifier.state, isA<AsyncData<List<ScheduledSavingsAccount>>>());
      expect(notifier.state.value, mockAccounts);
    });

    test('load sets state to error on failure', () async {
      final error = Exception('Failed to load accounts');
      when(() => dataSource.listAccounts()).thenThrow(error);

      await notifier.load();

      expect(notifier.state, isA<AsyncError<List<ScheduledSavingsAccount>>>());
    });

    test('create triggers a reload on success', () async {
      when(() => dataSource.createAccount(any())).thenAnswer((_) async => ScheduledSavingsAccount(
        id: 'acc-2',
        name: 'Nuevo Ahorro',
        status: 'ACTIVE',
        frequency: 'MONTHLY',
        contributionAmount: 100,
        currentBalance: 0,
        nextContributionDate: DateTime.now().toIso8601String(),
        progressPercentage: 0.0,
      ));
      when(() => dataSource.listAccounts()).thenAnswer((_) async => []);

      await notifier.create(CreateScheduledSavingsRequest(
        name: 'Nuevo Ahorro',
        frequency: 'MONTHLY',
        contributionAmount: 100,
        debitDayOfMonth: 1,
      ));

      verify(() => dataSource.createAccount(any())).called(1);
      verify(() => dataSource.listAccounts()).called(1);
    });
  });
}
