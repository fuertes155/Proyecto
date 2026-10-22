import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met/core/utils/currency_formatter.dart';
import 'package:met/features/loans/presentation/pages/loan_application_detail_page.dart';
import 'package:met/features/loans/presentation/pages/loan_applications_list_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

Map<String, dynamic> _installment(int number, String status) => {
      'installmentNumber': number,
      'paymentAmount': 1030000,
      'principalAmount': 1000000,
      'interestAmount': 30000,
      'remainingBalance': 3000000 - number * 1000000,
      'dueDate': '2026-1${number - 1}-15',
      'status': status,
      if (status == 'LATE') 'penaltyInterestAmount': 4500,
    };

final _application = {
  'id': 'loan-1',
  'amount': 3000000,
  'termMonths': 3,
  'monthlyPayment': 1030000,
  'totalPayment': 3090000,
  'purpose': 'Capital de trabajo',
  'status': 'DISBURSED',
  'submittedAt': '2026-09-01T10:00:00Z',
  'schedule': [_installment(1, 'PAID'), _installment(2, 'LATE'), _installment(3, 'PENDING')],
};

/// Solicitudes de crédito del socio: listado y detalle con el plan de pagos.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter()
      ..on('GET', '/v1/loans/applications', FakeResponse([_application]))
      ..on('GET', '/v1/loans/applications/loan-1', FakeResponse(_application));
  });

  Future<void> tapAndSettle(WidgetTester tester, Finder finder) async {
    await tester.ensureVisible(finder);
    await tester.pumpAndSettle();
    await tester.tap(finder);
    await tester.pumpAndSettle();
  }

  group('Listado', () {
    testWidgets('muestra cada solicitud y abre su detalle o el simulador', (tester) async {
      final router = await pumpScreen(tester, const LoanApplicationsListPage(),
          server: server, destinations: {'/loans/applications/:id', '/loans/simulate'});
      await tester.pumpAndSettle();

      expect(find.text(formatCop(3000000)), findsOneWidget);
      expect(find.text('3 meses · Desembolsada'), findsOneWidget);

      await tapAndSettle(tester, find.text(formatCop(3000000)));
      expect(currentPath(router), '/loans/applications/loan-1');

      router.pop();
      await tester.pumpAndSettle();
      await tapAndSettle(tester, find.byIcon(Icons.calculate));
      expect(currentPath(router), '/loans/simulate');
    });

    testWidgets('avisa cuando no hay solicitudes', (tester) async {
      server.on('GET', '/v1/loans/applications', const FakeResponse([]));
      await pumpScreen(tester, const LoanApplicationsListPage(), server: server);
      await tester.pumpAndSettle();

      expect(find.text('No tienes solicitudes de préstamo'), findsOneWidget);
    });

    testWidgets('explica el error sin texto técnico', (tester) async {
      server.on('GET', '/v1/loans/applications', const FakeResponse({}, status: 503));
      await pumpScreen(tester, const LoanApplicationsListPage(), server: server);
      await tester.pumpAndSettle();

      expect(find.text('Error en el servidor. Intenta más tarde.'), findsOneWidget);
      expect(find.textContaining('DioException'), findsNothing);
    });
  });

  group('Detalle', () {
    Future<void> openDetail(WidgetTester tester) async {
      await pumpScreen(tester, const LoanApplicationDetailPage(applicationId: 'loan-1'), server: server);
      await tester.pumpAndSettle();
    }

    testWidgets('muestra las condiciones y el plan de pagos con la cuota en mora resaltada', (tester) async {
      await openDetail(tester);

      expect(find.text('Desembolsada'), findsOneWidget);
      expect(find.text('Propósito: Capital de trabajo'), findsOneWidget);
      expect(find.text(formatCop(3090000)), findsOneWidget);
      expect(find.text('Cuota 1: ${formatCop(1030000)}'), findsOneWidget);
      expect(find.byIcon(Icons.check_circle), findsOneWidget, reason: 'la cuota pagada');
      expect(find.byIcon(Icons.warning), findsOneWidget, reason: 'la cuota en mora');
      expect(find.text('Interés moratorio: ${formatCop(4500)} (Tasa de usura)'), findsOneWidget);
    });

    testWidgets('explica el mandato firmado', (tester) async {
      await openDetail(tester);

      await tapAndSettle(tester, find.text('Ver Mandato Firmado'));
      expect(find.text('Contrato Protegido'), findsOneWidget);

      await tapAndSettle(tester, find.text('Cerrar'));
      expect(find.text('Contrato Protegido'), findsNothing);
    });

    testWidgets('explica el error sin texto técnico', (tester) async {
      server.on('GET', '/v1/loans/applications/loan-1', const FakeResponse({'message': 'Solicitud no encontrada'}, status: 404));
      await openDetail(tester);

      expect(find.text('Solicitud no encontrada'), findsOneWidget);
      expect(find.textContaining('DioException'), findsNothing);
    });
  });
}
