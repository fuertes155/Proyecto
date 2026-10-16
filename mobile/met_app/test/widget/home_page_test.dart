import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:met/core/theme/app_theme.dart';
import 'package:met/features/auth/data/models/auth_models.dart';
import 'package:met/features/auth/domain/repositories/auth_repository.dart';
import 'package:met/features/auth/presentation/pages/home_page.dart';
import 'package:met/features/auth/presentation/providers/auth_provider.dart';
import 'package:met/features/home/domain/entities/home_summary.dart';
import 'package:met/features/home/presentation/providers/home_provider.dart';
import 'package:met/features/notifications/presentation/providers/notification_provider.dart';
import 'package:mocktail/mocktail.dart';

class MockAuthRepository extends Mock implements AuthRepository {}

class FakeAuthNotifier extends AuthNotifier {
  FakeAuthNotifier(Ref ref, UserResponse user) : super(MockAuthRepository(), ref) {
    state = AsyncValue.data(user);
  }
}

class FakeHomeNotifier extends HomeNotifier {
  FakeHomeNotifier(this.summary);

  final HomeSummary summary;

  @override
  Future<HomeSummary> build() async => summary;
}

UserResponse buildUser({String email = 'juan@example.com', String kycStatus = 'APPROVED'}) => UserResponse(
      id: 'user-1',
      firstName: 'Juan Carlos',
      lastName: 'Pabón Gómez',
      email: email,
      phone: '3001234567',
      status: 'ACTIVE',
      kycStatus: kycStatus,
      emailNotificationsEnabled: true,
      pushNotificationsEnabled: true,
    );

HomeSummary buildSummary({List<RecentTransaction> transactions = const []}) => HomeSummary(
      userId: 'user-1',
      principalBalance: 2450000,
      interestBalance: 38750.5,
      accountNumber: '4510009283',
      accountStatus: 'ACTIVE',
      recentTransactions: transactions,
      savingsTotal: 600000,
      investmentsTotal: 1200000,
      pendingLoans: 1,
    );

void main() {
  setUpAll(() async {
    await initializeDateFormatting('es_CO', null);
  });

  Future<void> pumpHome(WidgetTester tester, {required UserResponse user, required HomeSummary summary}) async {
    tester.view.physicalSize = const Size(390 * 3, 1800 * 3);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);

    final router = GoRouter(
      initialLocation: '/home',
      routes: [GoRoute(path: '/home', builder: (context, state) => const HomePage())],
    );
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          authStateProvider.overrideWith((ref) => FakeAuthNotifier(ref, user)),
          homeProvider.overrideWith(() => FakeHomeNotifier(summary)),
          unreadNotificationsCountProvider.overrideWith((ref) async => 3),
        ],
        // Mismo tema de la app, sin descargar la fuente (en tests no hay red).
        child: MaterialApp.router(theme: AppTheme.lightThemeWith((t) => t), routerConfig: router),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('HomePage shows the real balance, account and quick actions', (WidgetTester tester) async {
    await pumpHome(tester, user: buildUser(), summary: buildSummary());

    expect(find.text('Juan'), findsOneWidget);
    expect(find.text('JP'), findsOneWidget);
    expect(find.textContaining('9283'), findsOneWidget);
    expect(find.text('\$ 2.450.000'), findsOneWidget);
    for (final action in ['Recargar', 'Transferir', 'Retirar', 'Más']) {
      expect(find.text(action), findsOneWidget);
    }
    expect(find.text('\$ 1.200.000'), findsOneWidget);
    expect(find.text('1 solicitud en revisión'), findsOneWidget);
    expect(find.text('3'), findsOneWidget); // notificaciones sin leer
    // Ya no se muestran datos inventados en la tarjeta.
    expect(find.textContaining('4092'), findsNothing);
    expect(find.textContaining('CVV'), findsNothing);
  });

  testWidgets('HomePage hides every amount when the balance is hidden', (WidgetTester tester) async {
    final semantics = tester.ensureSemantics();
    await pumpHome(tester, user: buildUser(), summary: buildSummary());

    await tester.tap(find.bySemanticsLabel('Ocultar saldo'));
    await tester.pumpAndSettle();

    expect(find.text('\$ 2.450.000'), findsNothing);
    expect(find.text('\$ 1.200.000'), findsNothing);
    expect(find.bySemanticsLabel('Mostrar saldo'), findsOneWidget);
    semantics.dispose();
  });

  testWidgets('HomePage lists recent transactions with sign and amount', (WidgetTester tester) async {
    final now = DateTime.now();
    await pumpHome(
      tester,
      user: buildUser(),
      summary: buildSummary(transactions: [
        RecentTransaction(id: '1', type: 'DEPOSIT', amount: 500000, description: 'Recarga PSE', createdAt: now, isCredit: true),
        RecentTransaction(id: '2', type: 'TRANSFER', amount: 120000, description: '', createdAt: now, isCredit: false),
      ]),
    );

    expect(find.text('Recarga PSE'), findsOneWidget);
    expect(find.text('+ \$ 500.000'), findsOneWidget);
    expect(find.text('Transferencia enviada'), findsOneWidget);
    expect(find.text('− \$ 120.000'), findsOneWidget);
    expect(find.text('Aún no tienes movimientos'), findsNothing);
  });

  testWidgets('HomePage asks for the email when the account has none', (WidgetTester tester) async {
    await pumpHome(tester, user: buildUser(email: '', kycStatus: 'IN_REVIEW'), summary: buildSummary());

    expect(find.text('Agrega tu correo'), findsOneWidget);
    expect(find.text('Estamos verificando tu identidad'), findsOneWidget);
    expect(find.text('Aún no tienes movimientos'), findsOneWidget);
  });

  testWidgets('HomePage shows no notices for a verified account with email', (WidgetTester tester) async {
    await pumpHome(tester, user: buildUser(), summary: buildSummary());

    expect(find.text('Agrega tu correo'), findsNothing);
    expect(find.text('Estamos verificando tu identidad'), findsNothing);
  });
}
