import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/features/transfers/presentation/pages/payment_webview_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

const _checkoutUrl = 'https://checkout.wompi.co/p/?public-key=pub_test_abc&currency=COP&amount-in-cents=5000000'
    '&reference=MET-550e8400-e29b-41d4-a716-446655440000-1730000000000&signature:integrity=abc';

/// Pantalla que abre el checkout de Wompi en el navegador del celular y espera a que el socio
/// vuelva con "Ya pagué". En el sandbox, el saldo solo sube cuando llega el webhook firmado.
void main() {
  late FakeDioAdapter server;
  late List<String> openedUrls;
  late bool browserOpens;

  setUp(() {
    mockNativePlugins(storage: {'user_id': 'user-1', 'access_token': 'acc-1'});
    openedUrls = [];
    browserOpens = true;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('plugins.flutter.io/url_launcher'), (call) async {
      if (call.method != 'launch') return true;
      openedUrls.add((call.arguments as Map)['url'] as String);
      return browserOpens;
    });
    // Sin ruta para el webhook simulado: así responde un backend con el sandbox real de Wompi.
    server = FakeDioAdapter();
  });

  /// La pantalla muestra un indicador girando mientras espera al socio: no hay "settle", así que
  /// se avanza el reloj por tramos fijos.
  Future<void> advance(WidgetTester tester) async {
    for (var i = 0; i < 5; i++) {
      await tester.pump(const Duration(milliseconds: 100));
    }
  }

  Future<GoRouter> openCheckout(WidgetTester tester) async {
    final router = await pumpScreen(tester,
        const PaymentWebViewPage(paymentUrl: _checkoutUrl, method: 'PSE', amount: 50000),
        server: server, destinations: {'/deposit/waiting'});
    await advance(tester);
    return router;
  }

  testWidgets('abre el checkout de Wompi en el navegador apenas entra', (tester) async {
    await openCheckout(tester);

    expect(openedUrls, [_checkoutUrl]);
    expect(find.textContaining('Se abrió la pasarela de pago de PSE'), findsOneWidget);
    expect(find.text('Ya pagué, continuar'), findsOneWidget);
  });

  testWidgets('si el navegador no abre, lo explica y deja reintentar', (tester) async {
    browserOpens = false;
    await openCheckout(tester);

    expect(find.textContaining('No se pudo abrir la pasarela de pago'), findsOneWidget);
    expect(find.text('Ya pagué, continuar'), findsNothing, reason: 'no hay pago que confirmar todavía');

    browserOpens = true;
    await tester.tap(find.text('Abrir pasarela de nuevo'));
    await advance(tester);

    expect(openedUrls, hasLength(2));
    expect(find.text('Ya pagué, continuar'), findsOneWidget);
  });

  testWidgets('"Ya pagué" lleva a esperar la confirmación con el monto y el medio de pago', (tester) async {
    final router = await openCheckout(tester);

    await tester.tap(find.text('Ya pagué, continuar'));
    await advance(tester);

    expect(currentPath(router), '/deposit/waiting');
    expect(currentExtra(router), {'method': 'PSE', 'amount': 50000.0});
  });

  testWidgets('con el sandbox real, el atajo de desarrollo no acredita nada y el flujo sigue', (tester) async {
    final router = await openCheckout(tester);

    await tester.tap(find.text('Ya pagué, continuar'));
    await advance(tester);

    // En compilaciones de desarrollo la app intenta el webhook simulado; el backend del sandbox no
    // lo tiene (404) y eso no debe atascar al socio: el saldo lo acredita el webhook real de Wompi.
    final simulated = server.requests.where((r) => r.path == '/v1/webhooks/mock-payment').toList();
    expect(simulated, hasLength(1));
    expect(currentPath(router), '/deposit/waiting');
  });

  testWidgets('la X cierra la pasarela sin confirmar nada', (tester) async {
    final router = await openCheckout(tester);

    await tester.tap(find.byIcon(Icons.close));
    await advance(tester);

    expect(currentPath(router), previousScreen);
  });
}
