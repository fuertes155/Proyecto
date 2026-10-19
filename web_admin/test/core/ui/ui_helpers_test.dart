import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met_admin/core/ui/ui.dart';

DioException _dio({int? status, DioExceptionType type = DioExceptionType.badResponse, Object? data}) {
  final options = RequestOptions(path: '/x');
  return DioException(
    requestOptions: options,
    type: type,
    response: status == null ? null : Response(requestOptions: options, statusCode: status, data: data),
  );
}

void main() {
  group('estados', () {
    test('las solicitudes de crédito nuevas (SUBMITTED) esperan decisión', () {
      expect(isLoanAwaitingDecision('SUBMITTED'), isTrue);
      expect(isLoanAwaitingDecision('IN_REVIEW'), isTrue);
      expect(isLoanAwaitingDecision('APPROVED'), isFalse);
      expect(loanStatus('SUBMITTED').label, 'Nueva');
    });

    test('KYC pendiente es todo lo que no está aprobado ni rechazado', () {
      expect(isKycPending('PENDING'), isTrue);
      expect(isKycPending('IN_REVIEW'), isTrue);
      expect(isKycPending('APPROVED'), isFalse);
      expect(isKycPending('REJECTED'), isFalse);
      expect(kycStatus('APPROVED').tone, Tone.success);
    });

    test('un código desconocido se muestra tal cual en tono neutro', () {
      expect(accountStatus('FROZEN').label, 'FROZEN');
      expect(accountStatus('FROZEN').tone, Tone.neutral);
    });
  });

  group('formatDateTime', () {
    test('formatea ISO en hora local y tolera nulos', () {
      final local = DateTime(2026, 10, 17, 14, 5);
      expect(formatDateTime(local.toIso8601String()), '17/10/2026 14:05');
      expect(formatDateTime(local.toIso8601String(), withTime: false), '17/10/2026');
      expect(formatDateTime(null), '—');
      expect(formatDateTime('no-es-fecha'), '—');
    });

    test('acepta segundos epoch (Jackson con timestamps)', () {
      final date = parseBackendDate(1735000000);
      expect(date, DateTime.fromMillisecondsSinceEpoch(1735000000000, isUtc: true));
    });
  });

  group('friendlyError', () {
    test('sesión expirada, permisos y conexión', () {
      expect(friendlyError(_dio(status: 401)), contains('sesión expiró'));
      expect(isSessionExpired(_dio(status: 401)), isTrue);
      expect(friendlyError(_dio(status: 403)), contains('permisos'));
      expect(friendlyError(_dio(type: DioExceptionType.connectionError)), contains('conectar con el servidor'));
    });

    test('usa el mensaje del backend cuando viene', () {
      expect(friendlyError(_dio(status: 422, data: {'message': 'El monto excede el límite'})), 'El monto excede el límite');
    });

    test('nunca muestra la excepción cruda', () {
      expect(friendlyError(Exception('boom')), isNot(contains('boom')));
    });
  });

  test('shortId acorta UUIDs', () {
    expect(shortId('7f3c9a10-1111-4c2d-9a8e-000000000001'), '7F3C9A10');
  });

  testWidgets('ResponsiveGrid evita dejar una tarjeta sola en la última fila', (tester) async {
    Future<List<double>> widthsAt(double width, int count) async {
      await tester.pumpWidget(MaterialApp(
        home: Center(
          child: SizedBox(
            width: width,
            child: ResponsiveGrid(
              minItemWidth: 160,
              spacing: 0,
              children: [for (var i = 0; i < count; i++) SizedBox(key: ValueKey(i), height: 10)],
            ),
          ),
        ),
      ));
      return [for (var i = 0; i < count; i++) tester.getSize(find.byKey(ValueKey(i))).width];
    }

    // 4 tarjetas donde caben 3 → 2 × 2.
    expect(await widthsAt(500, 4), everyElement(250));
    // 4 tarjetas donde caben 4 → una fila.
    expect(await widthsAt(800, 4), everyElement(200));
    // 3 tarjetas donde caben 2 → 2 + 1 (nunca una sola columna).
    expect(await widthsAt(340, 3), everyElement(170));
  });
}
