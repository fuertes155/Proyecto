import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met/core/network/api_client_provider.dart';
import 'package:met/features/home/presentation/providers/home_provider.dart';
import 'package:met/features/investment/presentation/providers/investment_providers.dart';
import 'package:met/features/loans/data/datasources/legal_remote_datasource.dart';
import 'package:met/features/loans/data/datasources/loan_remote_datasource.dart';
import 'package:met/features/loans/data/models/loan_models.dart';
import 'package:met/features/loans/presentation/providers/loan_provider.dart';
import 'package:met/features/notifications/presentation/providers/notification_provider.dart';
import 'package:met/features/reports/presentation/providers/reports_provider.dart';

import '../../helpers/fake_dio.dart';

Map<String, dynamic> _installment(int number, {String? status}) => {
      'installmentNumber': number,
      'paymentAmount': 1030000,
      'principalAmount': 1000000,
      'interestAmount': 30000,
      'remainingBalance': 3000000 - number * 1000000,
      'dueDate': '2026-1${number - 1}-15',
      if (status != null) 'status': status,
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
  'schedule': [_installment(1, status: 'PENDING'), _installment(2), _installment(3)],
};

/// Créditos, mandato, notificaciones, reportes, inversiones e inicio contra un servidor falso.
void main() {
  late FakeDioAdapter server;
  late ProviderContainer container;

  setUp(() {
    server = FakeDioAdapter();
    container = ProviderContainer(overrides: [apiClientProvider.overrideWithValue(fakeDio(server))]);
  });

  tearDown(() => container.dispose());

  group('Créditos', () {
    test('muestra el plan de pagos tal como lo envía el backend, sin cuotas en mora inventadas', () async {
      server.on('GET', '/v1/loans/applications/loan-1', FakeResponse(_application));

      final loan = await container.read(loanApplicationDetailProvider('loan-1').future);

      expect(loan.statusLabel, 'Desembolsada');
      expect(loan.schedule.map((i) => i.status), ['PENDING', 'PENDING', 'PENDING']);
      expect(loan.schedule.first.penaltyInterestAmount, 0.0);
    });

    test('simula un crédito con su tabla de amortización', () async {
      server.on('POST', '/v1/loans/simulate', FakeResponse({
        'amount': 3000000, 'termMonths': 3, 'annualInterestRate': 0.24, 'monthlyPayment': 1030000,
        'totalInterest': 90000, 'totalPayment': 3090000, 'schedule': [_installment(1), _installment(2), _installment(3)],
      }));
      final notifier = container.read(loanSimulationProvider.notifier);

      await notifier.simulate(SimulateLoanRequest(amount: 3000000, termMonths: 3));

      final result = container.read(loanSimulationProvider).value!;
      expect(server.last.json, {'amount': 3000000.0, 'termMonths': 3});
      expect(result.schedule.length, 3);
      expect(result.totalInterest, 90000);

      notifier.clear();
      expect(container.read(loanSimulationProvider).value, isNull);
    });

    test('consulta la elegibilidad aceptando Habeas Data y traduce la banda de riesgo', () async {
      server.on('POST', '/v1/loans/eligibility', const FakeResponse({
        'approved': true, 'tier': 'RIESGO_BAJO', 'score': 742, 'maxAmount': 8000000, 'maxTermMonths': 24, 'annualInterestRate': 0.21,
      }));
      container.listen(loanEligibilityProvider, (_, __) {});
      final notifier = container.read(loanEligibilityProvider.notifier);

      await notifier.fetch();

      expect(server.last.json, {'acceptedHabeasData': true});
      expect(container.read(loanEligibilityProvider).value!.tierLabel, 'Riesgo bajo');
      notifier.clear();
      expect(container.read(loanEligibilityProvider).value, isNull);
    });

    test('expone el error cuando la simulación o la elegibilidad fallan', () async {
      server
        ..on('POST', '/v1/loans/simulate', const FakeResponse({'message': 'Monto fuera de rango'}, status: 422))
        ..on('POST', '/v1/loans/eligibility', const FakeResponse({}, status: 503));

      container.listen(loanEligibilityProvider, (_, __) {});
      await container.read(loanSimulationProvider.notifier).simulate(SimulateLoanRequest(amount: 1, termMonths: 1));
      await container.read(loanEligibilityProvider.notifier).fetch();

      expect(container.read(loanSimulationProvider).hasError, isTrue);
      expect(container.read(loanEligibilityProvider).hasError, isTrue);
    });

    test('lista las solicitudes y envía una nueva con el consentimiento', () async {
      server
        ..on('GET', '/v1/loans/applications', FakeResponse([_application]))
        ..on('POST', '/v1/loans/applications', FakeResponse({..._application, 'status': 'IN_REVIEW', 'schedule': null}));

      final applications = await container.read(loanApplicationsProvider.future);
      final submitted = await container.read(loanRemoteDataSourceProvider).submitApplication(SubmitLoanApplicationRequest(
          amount: 3000000, termMonths: 3, purpose: 'Capital de trabajo', hasAcceptedHabeasData: true));

      expect(applications.single.id, 'loan-1');
      expect(server.last.json['hasAcceptedHabeasData'], isTrue);
      expect(submitted.statusLabel, 'En revisión');
      expect(submitted.schedule, isEmpty);
    });

    test('firma el mandato con el código recibido', () async {
      server
        ..on('POST', '/v1/legal/mandate/request-signature', const FakeResponse({'transactionId': 'tx_1'}))
        ..on('POST', '/v1/legal/mandate/confirm-signature', const FakeResponse({'contractId': 'c-1', 'hashSha256': 'abc'}));
      final legal = container.read(legalRemoteDataSourceProvider);

      final txId = await legal.requestSignature();
      final result = await legal.confirmSignature(txId, '482913');

      expect(server.last.json, {'transactionId': 'tx_1', 'otpCode': '482913'});
      expect(result['hashSha256'], 'abc');
    });
  });

  group('Notificaciones', () {
    test('carga la lista, cuenta las no leídas y marca una como leída sin recargar', () async {
      server
        ..on('GET', '/v1/notifications', const FakeResponse([
          {'id': 'n1', 'title': 'Abono', 'message': 'Recibiste \$50.000', 'read': false, 'createdAt': '2026-10-21T10:00:00Z'},
          {'id': 'n2', 'title': 'Bienvenido', 'message': 'Hola', 'read': true, 'createdAt': '2026-10-01T10:00:00Z'},
        ]))
        ..on('GET', '/v1/notifications/unread-count', const FakeResponse({'count': 1}))
        ..on('PUT', '/v1/notifications/n1/read', const FakeResponse({}));
      container.listen(notificationProvider, (_, __) {});

      await container.read(notificationProvider.notifier).fetchNotifications();
      expect(await container.read(unreadNotificationsCountProvider.future), 1);

      await container.read(notificationProvider.notifier).markAsRead('n1');

      expect(container.read(notificationProvider).value!.every((n) => n.read), isTrue);
      expect(server.requests.where((r) => r.path == '/v1/notifications').length, 2,
          reason: 'una carga al crear el notifier y la explícita; marcar como leída no recarga');
    });

    test('no rompe la lista si marcar como leída falla', () async {
      server
        ..on('GET', '/v1/notifications', const FakeResponse([
          {'id': 'n1', 'title': 'Abono', 'message': 'x', 'read': false, 'createdAt': '2026-10-21T10:00:00Z'},
        ]))
        ..on('PUT', '/v1/notifications/n1/read', const FakeResponse({}, status: 500));
      container.listen(notificationProvider, (_, __) {});
      await container.read(notificationProvider.notifier).fetchNotifications();

      await container.read(notificationProvider.notifier).markAsRead('n1');

      expect(container.read(notificationProvider).value!.single.read, isFalse);
    });
  });

  group('Reportes', () {
    test('descarga el reporte en bytes con el rango de fechas pedido', () async {
      server.on('GET', '/v1/reports/export', const FakeResponse('%PDF-1.7'));
      container.listen(reportExportProvider, (_, __) {});

      final bytes = await container.read(reportExportProvider.notifier)
          .export(from: DateTime(2026, 9, 1), to: DateTime(2026, 9, 30), format: 'pdf');

      expect(utf8.decode(bytes!), '%PDF-1.7');
      expect(server.last.queryParameters, {'from': '2026-09-01', 'to': '2026-09-30', 'format': 'pdf'});
      expect(container.read(reportExportProvider).isLoading, isFalse);
    });

    test('lee el mensaje del backend aunque el error llegue como bytes', () async {
      server.on('GET', '/v1/reports/export',
          const FakeResponse({'code': 'INVALID_DATE_RANGE', 'message': 'La fecha inicial debe ser anterior'}, status: 422));
      container.listen(reportExportProvider, (_, __) {});

      final bytes = await container.read(reportExportProvider.notifier)
          .export(from: DateTime(2026, 9, 30), to: DateTime(2026, 9, 1), format: 'xlsx');

      expect(bytes, isNull);
      expect(container.read(reportExportProvider).error, 'La fecha inicial debe ser anterior');
    });

    test('usa un mensaje genérico cuando el error no trae JSON', () async {
      server.on('GET', '/v1/reports/export', const FakeResponse('<html>502</html>', status: 502));
      container.listen(reportExportProvider, (_, __) {});

      await container.read(reportExportProvider.notifier).export(from: DateTime(2026, 9, 1), to: DateTime(2026, 9, 30), format: 'pdf');

      expect(container.read(reportExportProvider).error, 'No fue posible generar el reporte');
    });
  });

  group('Inversiones e inicio', () {
    test('resume en qué está trabajando el capital del socio', () async {
      server.on('GET', '/v1/investments/my-summary', const FakeResponse({
        'totalInvested': 50000, 'activeAmount': 20000, 'availableAmount': 10000, 'paidOffAmount': 10000,
        'returnedAmount': 10000, 'loansFundedCount': 2,
      }));

      final summary = await container.read(investmentSummaryProvider.future);

      expect(summary.activeAmount, 20000);
      expect(summary.loansFundedCount, 2);
    });

    test('carga el inicio y explica cuando el socio aún no tiene cuenta', () async {
      server.on('GET', '/v1/account/summary', const FakeResponse({'message': 'No se encontró una cuenta'}, status: 404));

      await expectLater(container.read(homeProvider.future), throwsA(predicate((e) => '$e'.contains('no tiene una cuenta'))));
    });
  });
}
