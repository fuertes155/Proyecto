import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/features/auth/presentation/pages/home_widgets/_more_actions_sheet.dart';
import 'package:met/features/auth/presentation/pages/home_widgets/quick_actions.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

/// Accesos rápidos del inicio (Recargar, Transferir, Retirar, Más) y la hoja "Más opciones".
void main() {
  late FakeDioAdapter server;

  setUp(() {
    mockNativePlugins();
    server = FakeDioAdapter();
  });

  group('Accesos rápidos', () {
    late int vecesMas;

    Future<GoRouter> abrirInicio(WidgetTester tester) {
      vecesMas = 0;
      return pumpScreen(
        tester,
        Scaffold(body: QuickActions(onTapMore: () => vecesMas++)),
        server: server,
        destinations: {'/deposit', '/transfer', '/accounts/external'},
      );
    }

    testWidgets('Transferir abre la transferencia', (tester) async {
      final router = await abrirInicio(tester);

      await tester.tap(find.text('Transferir'));
      await tester.pumpAndSettle();

      expect(currentPath(router), '/transfer');
    });

    testWidgets('Retirar abre las cuentas bancarias del socio', (tester) async {
      final router = await abrirInicio(tester);

      await tester.tap(find.text('Retirar'));
      await tester.pumpAndSettle();

      expect(currentPath(router), '/accounts/external');
    });

    testWidgets('Más le avisa al inicio para que abra el resto de opciones', (tester) async {
      await abrirInicio(tester);

      await tester.tap(find.text('Más'));
      await tester.pumpAndSettle();

      expect(vecesMas, 1);
    });

    testWidgets('Recargar muestra los medios de pago agrupados', (tester) async {
      await abrirInicio(tester);

      await tester.tap(find.text('Recargar'));
      await tester.pumpAndSettle();

      expect(find.text('Recargar tu cuenta'), findsOneWidget);
      for (final grupo in ['BANCO', 'TARJETAS', 'WALLETS', 'EFECTIVO']) {
        await tester.ensureVisible(find.text(grupo));
        expect(find.text(grupo), findsOneWidget, reason: grupo);
      }
    });

    const mediosDePago = <String, Map<String, String>>{
      'Nequi': {'method': 'Nequi'},
      'Bre-B': {'method': 'Bre-B'},
      'Bancolombia': {'method': 'PSE', 'bankHint': 'Bancolombia'},
      'Davivienda': {'method': 'PSE', 'bankHint': 'Davivienda'},
      'BBVA': {'method': 'PSE', 'bankHint': 'BBVA'},
      'Banco de Bogotá': {'method': 'PSE', 'bankHint': 'Bogotá'},
      'Otro banco (PSE)': {'method': 'PSE'},
      'Tarjeta de crédito/débito': {'method': 'Tarjeta de crédito/débito'},
      'Daviplata': {'method': 'Daviplata'},
      'Efecty': {'method': 'Efecty'},
      'Puntored': {'method': 'Puntored'},
      'Baloto': {'method': 'Baloto'},
      'Gana': {'method': 'Gana'},
      'Corresponsal Bancolombia': {'method': 'Corresponsal Bancolombia'},
    };

    for (final MapEntry(key: medio, value: datos) in mediosDePago.entries) {
      testWidgets('"$medio" cierra la hoja y abre el depósito con $datos', (tester) async {
        final router = await abrirInicio(tester);
        await tester.tap(find.text('Recargar'));
        await tester.pumpAndSettle();

        // Algunos medios muestran su nombre también en el logo: cualquiera de los dos abre el depósito.
        final opcion = find.text(medio).first;
        await tester.ensureVisible(opcion);
        await tester.pumpAndSettle();
        await tester.tap(opcion);
        await tester.pumpAndSettle();

        expect(currentPath(router), '/deposit');
        expect(currentExtra(router), datos);
        expect(find.text('Recargar tu cuenta'), findsNothing);
      });
    }
  });

  group('Más opciones', () {
    Future<GoRouter> abrirMenu(WidgetTester tester, Set<String> destinos) async {
      final router = await pumpScreen(
        tester,
        Scaffold(
          body: Builder(
            builder: (context) => TextButton(
              onPressed: () => MoreActionsSheet.show(context),
              child: const Text('Abrir más opciones'),
            ),
          ),
        ),
        server: server,
        destinations: destinos,
      );
      await tester.tap(find.text('Abrir más opciones'));
      await tester.pumpAndSettle();
      return router;
    }

    const opciones = <String, (String, String)>{
      'Mis movimientos': ('Extracto de transacciones', '/statement'),
      'Mis créditos': ('Solicitudes y estado', '/loans/applications'),
      'Inversiones': ('Tu portafolio', '/investments'),
      'Cuentas bancarias': ('Para tus retiros', '/accounts/external'),
      'Reportes': ('Descarga tus reportes', '/reports'),
      'Soporte 24/7': ('Chatea con nosotros', '/support/chat'),
    };

    testWidgets('muestra las seis opciones con su descripción', (tester) async {
      await abrirMenu(tester, const {});

      expect(find.text('Más opciones'), findsOneWidget);
      for (final MapEntry(key: titulo, value: (descripcion, _)) in opciones.entries) {
        expect(find.text(titulo), findsOneWidget, reason: titulo);
        expect(find.text(descripcion), findsOneWidget, reason: descripcion);
      }
    });

    for (final MapEntry(key: titulo, value: (_, ruta)) in opciones.entries) {
      testWidgets('"$titulo" cierra el menú y abre $ruta', (tester) async {
        final router = await abrirMenu(tester, {ruta});

        await tester.tap(find.text(titulo));
        await tester.pumpAndSettle();

        expect(currentPath(router), ruta);
        expect(find.text('Más opciones'), findsNothing);
      });
    }
  });
}
