import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../helpers/fake_dio.dart';
import '../helpers/panel_harness.dart';

const _laura = '7f3c9a10-1111-4c2d-9a8e-000000000001';
const _txId = '2b8d4e22-2222-4c2d-9a8e-000000000002';

/// Acciones críticas del panel: bloqueo de emergencia, reversión y reseteo de credenciales.
/// Todas piden confirmación explícita antes de llegar al servidor.
void main() {
  late FakeDioAdapter server;

  setUp(() {
    server = FakeDioAdapter()
      ..on('GET', '/users', const FakeResponse([
        {'id': _laura, 'firstName': 'Laura', 'lastName': 'Gómez', 'email': 'laura@example.com',
          'kycStatus': 'APPROVED', 'documentNumber': '1020456789', 'documentType': 'CC'},
      ]))
      ..on('POST', '/v1/admin/emergency-lock', const FakeResponse({}))
      ..on('POST', '/v1/admin/transactions/reverse', const FakeResponse({}))
      ..on('POST', '/v1/admin/users/$_laura/reset-credentials', const FakeResponse({}));
  });

  /// Busca al socio por su nombre y lo elige de las sugerencias.
  Future<void> pickLaura(WidgetTester tester) async {
    await tester.enterText(find.widgetWithText(TextFormField, 'Busca por nombre, documento o correo'), 'laura');
    await tester.pumpAndSettle();
    await tapAndSettle(tester, find.textContaining('Laura Gómez').last);
  }

  Future<void> writeReason(WidgetTester tester, String hint, String reason) async {
    await tester.enterText(find.widgetWithText(TextFormField, hint), reason);
    await tester.pump();
  }

  group('Bloqueo de emergencia', () {
    const reasonHint = 'Ej.: El socio reportó por la línea de soporte que le robaron el celular.';

    testWidgets('bloquea al socio elegido con el alcance y el motivo tras confirmar', (tester) async {
      await pumpPanel(tester, '/admin/emergency-lock', server: server);

      await pickLaura(tester);
      await tapAndSettle(tester, find.text('Cerrar todas sus sesiones'));
      await writeReason(tester, reasonHint, 'Robo del celular reportado a soporte');
      await tapAndSettle(tester, find.text('Ejecutar bloqueo'));
      expect(find.text('¿Ejecutar bloqueo de emergencia?'), findsOneWidget);
      await tapAndSettle(tester, find.text('Sí, bloquear'));

      expect(requestsTo(server, '/v1/admin/emergency-lock').single.json,
          {'targetId': _laura, 'scope': 'ACCESS_TOKEN', 'reason': 'Robo del celular reportado a soporte'});
      expect(find.text('Bloqueo de emergencia ejecutado'), findsOneWidget);
    });

    testWidgets('exige socio y un motivo de al menos 10 caracteres', (tester) async {
      await pumpPanel(tester, '/admin/emergency-lock', server: server);

      await writeReason(tester, reasonHint, 'robo');
      await tapAndSettle(tester, find.text('Ejecutar bloqueo'));

      expect(find.text('Describe el motivo (mínimo 10 caracteres)'), findsOneWidget);
      expect(find.text('¿Ejecutar bloqueo de emergencia?'), findsNothing);
      expect(requestsTo(server, '/v1/admin/emergency-lock'), isEmpty);
    });

    testWidgets('cancelar la confirmación no bloquea a nadie', (tester) async {
      await pumpPanel(tester, '/admin/emergency-lock', server: server);

      await pickLaura(tester);
      await writeReason(tester, reasonHint, 'Robo del celular reportado a soporte');
      await tapAndSettle(tester, find.text('Ejecutar bloqueo'));
      await tapAndSettle(tester, find.text('Cancelar'));

      expect(requestsTo(server, '/v1/admin/emergency-lock'), isEmpty);
    });
  });

  group('Reversión de transacción', () {
    testWidgets('exige el código CONFIRM- con el inicio del ID antes de registrar', (tester) async {
      await pumpPanel(tester, '/admin/transaction-reversal', server: server);

      await tester.enterText(find.widgetWithText(TextFormField, 'Ej.: 7f3c9a10-1111-4c2d-9a8e-000000000001'), _txId);
      await writeReason(tester, 'Ej.: Doble débito por reintento de la pasarela PSE.', 'Doble débito por reintento de PSE');
      await tester.pump();
      expect(find.text('Para confirmar, escribe exactamente: CONFIRM-2B8D4E22'), findsOneWidget);

      await tester.enterText(find.widgetWithText(TextFormField, 'CONFIRM-2B8D4E22'), 'confirm-otra');
      await tapAndSettle(tester, find.text('Registrar reversión'));
      expect(find.text('Escribe CONFIRM-2B8D4E22 para confirmar'), findsOneWidget);
      expect(requestsTo(server, '/v1/admin/transactions/reverse'), isEmpty);

      await tester.enterText(find.byType(TextFormField).last, 'confirm-2b8d4e22');
      await tapAndSettle(tester, find.text('Registrar reversión'));
      await tapAndSettle(tester, find.widgetWithText(FilledButton, 'Registrar reversión'));

      final sent = requestsTo(server, '/v1/admin/transactions/reverse').single.json;
      expect(sent['transactionId'], _txId);
      expect(sent['reason'], 'Doble débito por reintento de PSE');
      expect((sent['confirmationCode'] as String).toUpperCase(), 'CONFIRM-2B8D4E22',
          reason: 'el backend lo compara sin distinguir mayúsculas');
      expect(find.text('Reversión registrada en la auditoría'), findsOneWidget);
    });
  });

  group('Reseteo de credenciales', () {
    testWidgets('resetea el PIN del socio elegido tras confirmar', (tester) async {
      await pumpPanel(tester, '/admin/reset-credentials', server: server);

      await pickLaura(tester);
      await writeReason(tester, 'Ej.: Solicitud por pérdida del celular, identidad validada con videollamada.',
          'Pérdida del celular, validado por videollamada');
      await tapAndSettle(tester, find.text('Resetear credenciales').last);
      await tapAndSettle(tester, find.widgetWithText(FilledButton, 'Resetear credenciales'));

      expect(requestsTo(server, '/v1/admin/users/$_laura/reset-credentials').single.json,
          {'reason': 'Pérdida del celular, validado por videollamada'});
      expect(find.text('Credenciales reseteadas. El socio deberá registrar un PIN nuevo.'), findsOneWidget);
    });
  });
}
