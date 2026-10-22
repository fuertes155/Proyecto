import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/core/utils/currency_formatter.dart';
import 'package:met/features/loans/presentation/pages/loan_simulation_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

Map<String, dynamic> _eligibility({bool approved = true, double maxAmount = 8000000, int maxTermMonths = 24, String? reason}) => {
      'approved': approved,
      'tier': 'RIESGO_BAJO',
      'score': 742,
      'maxAmount': maxAmount,
      'maxTermMonths': maxTermMonths,
      'annualInterestRate': 0.21,
      if (reason != null) 'reason': reason,
    };

/// El simulador de crédito: autorización de Habeas Data, límites según DataCrédito, firma del
/// mandato con código y envío de la solicitud, contra un servidor falso.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter()
      ..on('POST', '/v1/loans/eligibility', FakeResponse(_eligibility()))
      ..on('POST', '/v1/legal/mandate/request-signature', const FakeResponse({'transactionId': 'tx_1'}))
      ..on('POST', '/v1/legal/mandate/confirm-signature', const FakeResponse({'contractId': 'c-1', 'hashSha256': 'abc'}))
      ..on('POST', '/v1/loans/applications', const FakeResponse({
        'id': 'loan-1', 'amount': 5000000, 'termMonths': 24, 'monthlyPayment': 256000, 'totalPayment': 6144000,
        'purpose': 'Capital de trabajo', 'status': 'IN_REVIEW', 'submittedAt': '2026-10-22T10:00:00Z',
      }));
  });

  Future<GoRouter> openSimulator(WidgetTester tester) async {
    final router = await pumpScreen(tester, const LoanSimulationPage(),
        server: server, destinations: {'/loans/applications', '/loans/applications/:id'});
    // Detrás del diálogo de autorización hay un indicador de carga girando: no hay "settle".
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));
    return router;
  }

  Future<void> tapAndSettle(WidgetTester tester, Finder finder) async {
    await tester.ensureVisible(finder);
    await tester.pumpAndSettle();
    await tester.tap(finder);
    await tester.pumpAndSettle();
  }

  /// Responde el diálogo de Habeas Data (detrás gira el indicador de carga: se toca sin esperar).
  Future<void> answerConsent(WidgetTester tester, String answer) async {
    await tester.tap(find.text(answer));
    await tester.pumpAndSettle();
  }

  Future<void> authorize(WidgetTester tester) => answerConsent(tester, 'Autorizo la consulta');

  List<RecordedRequest> requestsTo(String path) => server.requests.where((r) => r.path == path).toList();

  testWidgets('pide autorización de Habeas Data antes de consultar DataCrédito', (tester) async {
    await openSimulator(tester);

    expect(find.text('Autorización Habeas Data'), findsOneWidget);
    expect(requestsTo('/v1/loans/eligibility'), isEmpty);

    await authorize(tester);

    expect(requestsTo('/v1/loans/eligibility').single.json, {'acceptedHabeasData': true});
    expect(find.text('Según tu perfil (Riesgo bajo) en DataCrédito'), findsOneWidget);
    expect(find.text('21.0%'), findsOneWidget);
    expect(find.text(formatCop(5000000)), findsOneWidget);
    expect(find.text('24 meses'), findsWidgets);
  });

  testWidgets('si no autoriza, vuelve atrás sin consultar', (tester) async {
    final router = await openSimulator(tester);

    await answerConsent(tester, 'No autorizo');

    expect(currentPath(router), previousScreen);
    expect(requestsTo('/v1/loans/eligibility'), isEmpty);
  });

  testWidgets('encuadra el simulador dentro de los límites del perfil', (tester) async {
    server.on('POST', '/v1/loans/eligibility', FakeResponse(_eligibility(maxAmount: 3000000, maxTermMonths: 12)));
    await openSimulator(tester);

    await authorize(tester);

    expect(find.text(formatCop(3000000)), findsWidgets);
    expect(find.text('12 meses'), findsWidgets);
    final amountSlider = tester.widget<Slider>(find.byType(Slider).first);
    expect(amountSlider.max, 3000000);
    expect(amountSlider.value, 3000000);
  });

  testWidgets('explica por qué no puede pedir un crédito', (tester) async {
    server.on('POST', '/v1/loans/eligibility',
        FakeResponse(_eligibility(approved: false, reason: 'Tu ahorro aún no alcanza el mínimo requerido')));
    final router = await openSimulator(tester);
    await authorize(tester);

    expect(find.text('Por ahora no puedes solicitar un préstamo'), findsOneWidget);
    expect(find.text('Tu ahorro aún no alcanza el mínimo requerido'), findsOneWidget);
    expect(find.byType(Slider), findsNothing);

    await tapAndSettle(tester, find.text('Volver'));
    expect(currentPath(router), previousScreen);
  });

  testWidgets('permite reintentar si DataCrédito no responde', (tester) async {
    server.on('POST', '/v1/loans/eligibility', const FakeResponse({'message': 'Servicio no disponible'}, status: 503));
    await openSimulator(tester);
    await authorize(tester);
    expect(find.text('Reintentar'), findsOneWidget);

    server.on('POST', '/v1/loans/eligibility', FakeResponse(_eligibility()));
    await tapAndSettle(tester, find.text('Reintentar'));

    expect(find.text('¿Cuánto dinero necesitas?'), findsOneWidget);
  });

  testWidgets('firma el mandato con el código del correo y envía la solicitud', (tester) async {
    final router = await openSimulator(tester);
    await authorize(tester);

    await tester.enterText(find.widgetWithText(TextFormField, 'Propósito del préstamo'), 'Capital de trabajo');
    await tester.pumpAndSettle();
    await tapAndSettle(tester, find.text('Solicitar Préstamo'));
    expect(find.text('Firma de Mandato Legal'), findsOneWidget);

    await tester.enterText(find.widgetWithText(TextField, 'Código OTP (6 dígitos)'), '482913');
    await tapAndSettle(tester, find.text('Firmar y Continuar'));

    expect(requestsTo('/v1/legal/mandate/confirm-signature').single.json, {'transactionId': 'tx_1', 'otpCode': '482913'});
    expect(requestsTo('/v1/loans/applications').single.json, {
      'amount': 5000000.0,
      'termMonths': 24,
      'purpose': 'Capital de trabajo',
      'hasAcceptedHabeasData': true,
      'annualInterestRate': 0.21,
    });
    expect(currentPath(router), '/loans/applications/loan-1');
  });

  testWidgets('no pide la firma sin propósito ni envía nada si se cancela', (tester) async {
    await openSimulator(tester);
    await authorize(tester);

    await tapAndSettle(tester, find.text('Solicitar Préstamo'));
    expect(find.text('Indica el propósito del préstamo'), findsOneWidget);
    expect(requestsTo('/v1/legal/mandate/request-signature'), isEmpty);
    await tester.pump(const Duration(seconds: 5)); // se va el aviso que tapaba el botón
    await tester.pumpAndSettle();

    await tester.enterText(find.widgetWithText(TextFormField, 'Propósito del préstamo'), 'Estudio');
    await tester.pumpAndSettle();
    await tapAndSettle(tester, find.text('Solicitar Préstamo'));
    await tapAndSettle(tester, find.text('Cancelar'));

    expect(requestsTo('/v1/legal/mandate/confirm-signature'), isEmpty);
    expect(requestsTo('/v1/loans/applications'), isEmpty);
  });

  testWidgets('muestra el error si la firma falla', (tester) async {
    server.on('POST', '/v1/legal/mandate/confirm-signature',
        const FakeResponse({'code': 'INVALID_OTP', 'message': 'El código es incorrecto'}, status: 422));
    await openSimulator(tester);
    await authorize(tester);
    await tester.enterText(find.widgetWithText(TextFormField, 'Propósito del préstamo'), 'Estudio');
    await tester.pumpAndSettle();
    await tapAndSettle(tester, find.text('Solicitar Préstamo'));

    await tester.enterText(find.widgetWithText(TextField, 'Código OTP (6 dígitos)'), '000000');
    await tapAndSettle(tester, find.text('Firmar y Continuar'));

    expect(find.byType(SnackBar), findsOneWidget);
    expect(requestsTo('/v1/loans/applications'), isEmpty);
    expect(tester.widget<ElevatedButton>(find.widgetWithText(ElevatedButton, 'Solicitar Préstamo')).onPressed, isNotNull,
        reason: 'el botón vuelve a quedar disponible');
  });

  testWidgets('al volver a abrirlo no muestra la consulta anterior antes de autorizar', (tester) async {
    server.on('POST', '/v1/loans/eligibility', FakeResponse(_eligibility(maxAmount: 3000000, maxTermMonths: 12)));
    final router = await openSimulator(tester);
    await authorize(tester);
    router.pop();
    await tester.pumpAndSettle();

    router.push('/screen');
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));

    expect(find.text('Autorización Habeas Data'), findsOneWidget);
    expect(find.textContaining('Según tu perfil'), findsNothing,
        reason: 'el resultado de DataCrédito es de la consulta anterior (quizá de otro socio)');
  });

  testWidgets('abre la lista de solicitudes', (tester) async {
    final router = await openSimulator(tester);
    await authorize(tester);

    await tapAndSettle(tester, find.byTooltip('Mis solicitudes'));

    expect(currentPath(router), '/loans/applications');
  });
}
