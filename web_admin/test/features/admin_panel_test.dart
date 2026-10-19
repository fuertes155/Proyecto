import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met_admin/core/router/app_router.dart';
import 'package:met_admin/core/theme/app_theme.dart';
import 'package:met_admin/features/admin/providers/admin_business_provider.dart';

final _users = [
  UserModel(id: 'u1', firstName: 'Laura', lastName: 'Gómez', email: 'laura@example.com', kycStatus: 'PENDING', documentNumber: '1020456789'),
  UserModel(id: 'u2', firstName: 'Andrés', lastName: 'Martínez', email: 'andres@example.com', kycStatus: 'APPROVED', documentNumber: '79845112'),
];

final _loans = [
  LoanModel(id: '7f3c9a10-1111-4c2d-9a8e-000000000001', userId: 'u2', status: 'SUBMITTED', amount: 5000000, termMonths: 12),
  LoanModel(id: '2b8d4e22-2222-4c2d-9a8e-000000000002', userId: 'u1', status: 'APPROVED', amount: 3000000, termMonths: 6),
];

Future<ProviderContainer> _pumpPanel(WidgetTester tester, String route) async {
  tester.view.physicalSize = const Size(1440, 1000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  final container = ProviderContainer(overrides: [
    usersProvider.overrideWith((ref) async => _users),
    loansProvider.overrideWith((ref) async => _loans),
  ]);
  addTearDown(container.dispose);
  final router = container.read(appRouterProvider);
  await tester.pumpWidget(UncontrolledProviderScope(
    container: container,
    child: MaterialApp.router(theme: AppTheme.lightTheme, routerConfig: router),
  ));
  router.go(route);
  await tester.pumpAndSettle();
  return container;
}

void main() {
  testWidgets('el panel muestra el menú lateral con el eslogan y marca la sección activa', (tester) async {
    await _pumpPanel(tester, '/admin/partners');

    expect(find.text('Impulso instantáneo, confianza permanente'), findsOneWidget);
    expect(find.text('OPERACIONES'), findsWidgets);
    // Título de la página + elemento del menú.
    expect(find.text('Socios y KYC'), findsNWidgets(2));
    // Por defecto abre en los KYC por revisar: solo Laura.
    expect(find.text('Laura Gómez'), findsOneWidget);
    expect(find.text('Andrés Martínez'), findsNothing);
  });

  testWidgets('aprobar un KYC pide confirmación con los datos del socio', (tester) async {
    await _pumpPanel(tester, '/admin/partners');

    await tester.tap(find.text('Aprobar'));
    await tester.pumpAndSettle();

    expect(find.text('Aprobar KYC'), findsOneWidget);
    expect(find.text('CC 1020456789'), findsWidgets);

    await tester.tap(find.text('Cancelar'));
    await tester.pumpAndSettle();
    expect(find.text('Aprobar KYC'), findsNothing);
  });

  testWidgets('las solicitudes de crédito nuevas (SUBMITTED) se pueden decidir', (tester) async {
    await _pumpPanel(tester, '/admin/loans');

    expect(find.text('Nueva'), findsOneWidget);
    expect(find.text('Aprobar'), findsOneWidget);
    expect(find.text('Rechazar'), findsOneWidget);
  });

  testWidgets('el menú navega entre secciones', (tester) async {
    await _pumpPanel(tester, '/admin/partners');

    await tester.tap(find.text('Solicitudes de crédito').first);
    await tester.pumpAndSettle();

    expect(find.text('Evalúa cada solicitud con su monto, plazo y nivel de riesgo antes de decidir.'), findsOneWidget);
  });
}
