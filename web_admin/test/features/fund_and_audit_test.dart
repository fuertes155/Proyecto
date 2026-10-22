import 'package:flutter_test/flutter_test.dart';
import 'package:met_admin/core/utils/currency_formatter.dart';

import '../helpers/fake_dio.dart';
import '../helpers/panel_harness.dart';

Map<String, dynamic> _movement(String id, String type, double amount) => {
      'id': id,
      'type': type,
      'amount': amount,
      'concept': type == 'CONTRIBUTION' ? 'Aporte del crédito $id' : 'Cobertura por mora $id',
      'createdAt': '2026-10-20T15:00:00Z',
    };

Map<String, dynamic> _entry(int n, String action) => {
      'id': 'e$n',
      'actorAdminId': 'admin-1',
      'accion': action,
      'entidadAfectada': 'USER',
      'idEntidad': 'u$n',
      'motivo': n.isEven ? 'Motivo $n' : null,
      'ipOrigen': '190.85.10.$n',
      'timestamp': '2026-10-20T15:00:00Z',
    };

/// Fondo de garantías y bitácora de auditoría del panel, contra un servidor falso.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    server = FakeDioAdapter()
      ..on('GET', '/v1/admin/investments/guarantee-fund', FakeResponse({
        'balance': 12500000,
        'coverageRatio': 0.8,
        'activationDaysLate': 90,
        'movements': [
          _movement('m1', 'CONTRIBUTION', 300000),
          _movement('m2', 'CONTRIBUTION', 200000),
          _movement('m3', 'PAYOUT', 150000),
        ],
      }))
      ..on('GET', '/v1/admin/audit-log?page=0&size=20', FakeResponse({
        'data': [for (var n = 0; n < 20; n++) _entry(n, n == 0 ? 'EMERGENCY_LOCK_USER' : 'FEE_SCHEDULE_UPDATED')],
        'total': 25,
        'page': 0,
        'size': 20,
      }))
      ..on('GET', '/v1/admin/audit-log?page=1&size=20', FakeResponse({
        'data': [for (var n = 20; n < 25; n++) _entry(n, 'TRANSACTION_REVERSED')],
        'total': 25,
        'page': 1,
        'size': 20,
      }));
  });

  group('Fondo de garantías', () {
    testWidgets('muestra el saldo, las reglas de cobertura y el total por tipo de movimiento', (tester) async {
      await pumpPanel(tester, '/admin/guarantee-fund', server: server);

      expect(find.text(formatCop(12500000)), findsWidgets);
      expect(find.text('80% del saldo en mora'), findsOneWidget);
      expect(find.text('90 días de mora'), findsOneWidget);
      expect(find.text(formatCop(500000)), findsOneWidget, reason: 'aportes recibidos');
      expect(find.text(formatCop(150000)), findsWidgets, reason: 'coberturas pagadas');
      expect(find.text('Aporte del crédito m1'), findsOneWidget);
    });

    testWidgets('sin movimientos explica de dónde llegan los aportes', (tester) async {
      server.on('GET', '/v1/admin/investments/guarantee-fund',
          const FakeResponse({'balance': 0, 'coverageRatio': 0.8, 'activationDaysLate': 90, 'movements': []}));
      await pumpPanel(tester, '/admin/guarantee-fund', server: server);

      expect(find.text('Todavía no hay movimientos'), findsOneWidget);
    });
  });

  group('Auditoría', () {
    testWidgets('traduce las acciones y pagina los registros', (tester) async {
      await pumpPanel(tester, '/admin/audit-log', server: server);

      expect(find.text('Bloqueo de usuario'), findsOneWidget);
      expect(find.text('Mostrando 1–20 de 25'), findsOneWidget);

      await tapAndSettle(tester, find.text('Siguiente'));
      expect(find.text('Mostrando 21–25 de 25'), findsOneWidget);
      expect(find.text('Reversión registrada'), findsWidgets);

      await tapAndSettle(tester, find.text('Anterior'));
      expect(find.text('Mostrando 1–20 de 25'), findsOneWidget);
    });

    testWidgets('sin registros lo dice', (tester) async {
      server.on('GET', '/v1/admin/audit-log?page=0&size=20', const FakeResponse({'data': [], 'total': 0, 'page': 0, 'size': 20}));
      await pumpPanel(tester, '/admin/audit-log', server: server);

      expect(find.text('Todavía no hay registros de auditoría'), findsOneWidget);
    });
  });
}
