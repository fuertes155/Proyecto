import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met_admin/core/utils/currency_formatter.dart';

import '../helpers/fake_dio.dart';
import '../helpers/panel_harness.dart';

Map<String, dynamic> _user(String id, String name, String kyc) =>
    {'id': id, 'firstName': name, 'lastName': 'Pérez', 'email': '$id@example.com', 'kycStatus': kyc, 'documentNumber': '10$id'};

Map<String, dynamic> _account(String id, String userId, String status, double balance) =>
    {'id': id, 'accountNumber': '451000$id', 'userId': userId, 'status': status, 'principalBalance': balance};

/// Tablero del panel y listado de cuentas, alimentados por la API de negocio y la de administración.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    server = FakeDioAdapter()
      ..on('GET', '/users', FakeResponse([
        _user('u1', 'Laura', 'PENDING'),
        _user('u2', 'Andrés', 'APPROVED'),
        _user('u3', 'Sofía', 'IN_REVIEW'),
      ]))
      ..on('GET', '/accounts', FakeResponse([
        _account('1', 'u1', 'ACTIVE', 1500000),
        _account('2', 'u2', 'ACTIVE', 2500000),
        _account('3', 'u3', 'BLOCKED', 1000000),
      ]))
      ..on('GET', '/loans', const FakeResponse([
        {'id': 'l1', 'userId': 'u2', 'status': 'SUBMITTED', 'amount': 5000000, 'termMonths': 12},
        {'id': 'l2', 'userId': 'u1', 'status': 'APPROVED', 'amount': 3000000, 'termMonths': 6},
      ]))
      ..on('GET', '/v1/admin/investments/guarantee-fund',
          const FakeResponse({'balance': 12500000, 'coverageRatio': 0.8, 'activationDaysLate': 90, 'movements': []}))
      ..on('GET', '/v1/admin/maintenance', const FakeResponse([
        {'id': 'm1', 'descripcion': 'Migración', 'inicio': '2026-10-25T08:00:00Z', 'fin': '2026-10-25T10:00:00Z', 'activo': true},
      ]))
      ..on('GET', '/v1/admin/compliance/alerts?status=OPEN&page=0&size=20', const FakeResponse({'items': [], 'total': 4}));
  });

  group('Tablero', () {
    testWidgets('resume socios, saldos, créditos y fondo', (tester) async {
      await pumpPanel(tester, '/admin/dashboard', server: server);

      expect(find.text('2 con KYC pendiente'), findsOneWidget);
      expect(find.text(formatCop(5000000)), findsWidgets, reason: 'saldo total de las cuentas');
      expect(find.text('2 cuentas activas'), findsOneWidget);
      expect(find.text('2 solicitudes en total'), findsOneWidget);
      expect(find.text('Cubre hasta el 80% en mora'), findsOneWidget);
    });

    testWidgets('lista lo que requiere atención y lleva a cada sección', (tester) async {
      final container = await pumpPanel(tester, '/admin/dashboard', server: server);

      expect(find.text('Requiere tu atención'), findsOneWidget);
      expect(find.text('Alertas SARLAFT abiertas'), findsOneWidget);
      expect(find.text('4'), findsWidgets);

      await tapAndSettle(tester, find.text('Revisar KYC pendientes'));
      expect(panelPath(container), '/admin/partners');
    });

    testWidgets('si una fuente falla, el resto del tablero sigue funcionando', (tester) async {
      server.on('GET', '/accounts', const FakeResponse({}, status: 500));
      await pumpPanel(tester, '/admin/dashboard', server: server);

      expect(find.text('2 con KYC pendiente'), findsOneWidget);
      expect(find.text('Cubre hasta el 80% en mora'), findsOneWidget);
    });
  });

  group('Cuentas', () {
    testWidgets('muestra cada cuenta con su titular y totales', (tester) async {
      await pumpPanel(tester, '/admin/accounts', server: server);

      expect(find.text('Laura Pérez'), findsOneWidget);
      expect(find.text('4510001'), findsOneWidget);
      expect(find.text(formatCop(5000000)), findsOneWidget);
    });

    testWidgets('filtra por estado y busca por titular o número', (tester) async {
      await pumpPanel(tester, '/admin/accounts', server: server);

      await tapAndSettle(tester, find.text('Bloqueadas'));
      expect(find.text('Sofía Pérez'), findsOneWidget);
      expect(find.text('Laura Pérez'), findsNothing);

      await tapAndSettle(tester, find.text('Todas'));
      await tester.enterText(find.byType(TextField).first, 'andrés');
      await tester.pumpAndSettle();
      expect(find.text('Andrés Pérez'), findsOneWidget);
      expect(find.text('Sofía Pérez'), findsNothing);
    });
  });
}
