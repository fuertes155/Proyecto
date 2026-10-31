import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/core/network/api_client_provider.dart';
import 'package:met/core/router/app_router.dart';
import 'package:met/features/auth/data/models/auth_models.dart';
import 'package:met/features/auth/data/repositories/auth_repository_impl.dart';
import 'package:met/features/auth/presentation/providers/auth_provider.dart';
import 'package:met/features/auth/presentation/pages/biometric_registration_page.dart';
import 'package:met/features/auth/presentation/pages/email_verification_page.dart';
import 'package:met/features/auth/presentation/pages/home_page.dart';
import 'package:met/features/auth/presentation/pages/login_page.dart';
import 'package:met/features/auth/presentation/pages/notifications_settings_page.dart';
import 'package:met/features/auth/presentation/pages/personal_data_page.dart';
import 'package:met/features/auth/presentation/pages/pin_recovery_page.dart';
import 'package:met/features/auth/presentation/pages/profile_page.dart';
import 'package:met/features/auth/presentation/pages/register_page.dart';
import 'package:met/features/auth/presentation/pages/security_settings_page.dart';
import 'package:met/features/auth/presentation/pages/splash_page.dart';
import 'package:met/features/external_accounts/presentation/pages/add_external_bank_account_page.dart';
import 'package:met/features/external_accounts/presentation/pages/external_bank_accounts_page.dart';
import 'package:met/features/external_accounts/presentation/pages/payout_page.dart';
import 'package:met/features/investment/presentation/pages/investment_breakdown_page.dart';
import 'package:met/features/loans/presentation/pages/loan_application_detail_page.dart';
import 'package:met/features/loans/presentation/pages/loan_applications_list_page.dart';
import 'package:met/features/loans/presentation/pages/loan_simulation_page.dart';
import 'package:met/features/notifications/presentation/pages/notifications_page.dart';
import 'package:met/features/reports/presentation/pages/reports_page.dart';
import 'package:met/features/support/presentation/pages/chat_page.dart';
import 'package:met/features/transfers/presentation/pages/account_statement_page.dart';
import 'package:met/features/transfers/presentation/pages/deposit_page.dart';
import 'package:met/features/transfers/presentation/pages/deposit_waiting_page.dart';
import 'package:met/features/transfers/presentation/pages/payment_webview_page.dart';
import 'package:met/features/transfers/presentation/pages/transfer_page.dart';

import '../helpers/fake_dio.dart';
import '../helpers/page_harness.dart';

/// Cada ruta de la app arma su pantalla con los datos que recibe al navegar (extra y parámetros de
/// la ruta). Se llama al constructor de cada ruta sin abrir la pantalla.
void main() {
  late ProviderContainer container;
  late GoRouter router;
  late BuildContext context;

  const pantallasSinDatos = <String, Type>{
    '/': SplashPage,
    '/login': LoginPage,
    '/register': RegisterPage,
    '/biometric-registration': BiometricRegistrationPage,
    '/home': HomePage,
    '/profile': ProfilePage,
    '/profile/personal-data': PersonalDataPage,
    '/profile/security': SecuritySettingsPage,
    '/profile/notifications': NotificationsSettingsPage,
    '/notifications': NotificationsPage,
    '/transfer': TransferPage,
    '/auth/recover-pin': PinRecoveryPage,
    '/accounts/external': ExternalBankAccountsPage,
    '/accounts/external/add': AddExternalBankAccountPage,
    '/loans/simulate': LoanSimulationPage,
    '/loans/applications': LoanApplicationsListPage,
    '/statement': AccountStatementPage,
    '/reports': ReportsPage,
    '/investments': InvestmentBreakdownPage,
    '/support/chat': ChatPage,
  };
  const pantallasConDatos = {
    '/verify-email', '/deposit', '/deposit/waiting', '/deposit/webview', '/payout', '/loans/applications/:applicationId',
  };

  setUp(() {
    container = ProviderContainer();
    router = container.read(appRouterProvider);
  });

  tearDown(() {
    router.dispose();
    container.dispose();
  });

  Future<void> conContexto(WidgetTester tester) async {
    await tester.pumpWidget(const SizedBox());
    context = tester.element(find.byType(SizedBox));
  }

  Widget abrir(String ruta, {Object? extra, Map<String, String> parametros = const {}}) {
    final route = router.configuration.routes.whereType<GoRoute>().singleWhere((r) => r.path == ruta);
    return route.builder!(
      context,
      GoRouterState(router.configuration,
          uri: Uri.parse(ruta),
          matchedLocation: ruta,
          fullPath: ruta,
          pathParameters: parametros,
          extra: extra,
          pageKey: ValueKey(ruta)),
    );
  }

  testWidgets('cada ruta está probada aquí: una ruta nueva debe agregarse a esta prueba', (tester) async {
    final rutas = router.configuration.routes.whereType<GoRoute>().map((r) => r.path).toSet();

    expect(rutas, {...pantallasSinDatos.keys, ...pantallasConDatos});
  });

  testWidgets('la app arranca en la pantalla de bienvenida', (tester) async {
    expect(router.routeInformationProvider.value.uri.path, '/');
  });

  testWidgets('las rutas sin datos abren su pantalla', (tester) async {
    await conContexto(tester);

    for (final MapEntry(key: ruta, value: tipo) in pantallasSinDatos.entries) {
      expect(abrir(ruta).runtimeType, tipo, reason: ruta);
    }
  });

  testWidgets('la verificación del correo recibe el documento con el que se ingresó', (tester) async {
    await conContexto(tester);

    final pagina = abrir('/verify-email', extra: {'documentType': 'CE', 'documentNumber': '1086578123'})
        as EmailVerificationPage;

    expect(pagina.documentType, 'CE');
    expect(pagina.documentNumber, '1086578123');
  });

  testWidgets('sin datos, la verificación del correo usa la cédula (CC), un tipo que el servidor acepta',
      (tester) async {
    await conContexto(tester);

    final pagina = abrir('/verify-email') as EmailVerificationPage;

    // El servidor solo acepta CC, CE, TI y PASAPORTE.
    expect(pagina.documentType, 'CC');
    expect(pagina.documentNumber, '');
  });

  testWidgets('el depósito recibe el medio de pago y el banco sugerido', (tester) async {
    await conContexto(tester);

    final conDatos = abrir('/deposit', extra: {'method': 'PSE', 'bankHint': 'Bancolombia'}) as DepositPage;
    final sinDatos = abrir('/deposit') as DepositPage;

    expect((conDatos.method, conDatos.bankHint), ('PSE', 'Bancolombia'));
    expect((sinDatos.method, sinDatos.bankHint), ('Nequi', null));
  });

  testWidgets('la espera del depósito recibe el medio de pago y el monto', (tester) async {
    await conContexto(tester);

    final conDatos = abrir('/deposit/waiting', extra: {'method': 'PSE', 'amount': 50000.0}) as DepositWaitingPage;
    final sinDatos = abrir('/deposit/waiting') as DepositWaitingPage;

    expect((conDatos.method, conDatos.amount), ('PSE', 50000.0));
    expect((sinDatos.method, sinDatos.amount), ('Nequi', 0.0));
  });

  testWidgets('el pago en Wompi recibe el enlace, el medio de pago y el monto', (tester) async {
    await conContexto(tester);

    final conDatos = abrir('/deposit/webview',
        extra: {'paymentUrl': 'https://checkout.wompi.co/l/abc123', 'method': 'PSE', 'amount': 80000.0})
        as PaymentWebViewPage;
    final sinDatos = abrir('/deposit/webview') as PaymentWebViewPage;

    expect((conDatos.paymentUrl, conDatos.method, conDatos.amount), ('https://checkout.wompi.co/l/abc123', 'PSE', 80000.0));
    expect((sinDatos.paymentUrl, sinDatos.method, sinDatos.amount), ('', 'Pago', 0.0));
  });

  testWidgets('el retiro abre con la cuenta bancaria elegida, si la hay', (tester) async {
    await conContexto(tester);

    expect((abrir('/payout', extra: {'accountId': 'cuenta-7'}) as PayoutPage).preselectedAccountId, 'cuenta-7');
    expect((abrir('/payout') as PayoutPage).preselectedAccountId, isNull);
  });

  group('Protección de rutas', () {
    late GoRouter protegido;
    late ProviderContainer contenedor;

    /// Enrutador real con la sesión en [estado].
    Future<void> conSesion(WidgetTester tester, AsyncValue<UserResponse?> estado) async {
      mockNativePlugins();
      contenedor = ProviderContainer(overrides: [
        apiClientProvider.overrideWithValue(fakeDio(FakeDioAdapter())),
        authStateProvider.overrideWith((ref) => _SesionFija(ref, estado)),
      ]);
      protegido = contenedor.read(appRouterProvider);
      addTearDown(() {
        protegido.dispose();
        contenedor.dispose();
      });
      await conContexto(tester);
    }

    Future<String?> redirigeA(String ruta) async => protegido.configuration.topRedirect(
          context,
          GoRouterState(protegido.configuration,
              uri: Uri.parse(ruta), matchedLocation: ruta, fullPath: ruta, pathParameters: const {}, pageKey: ValueKey(ruta)),
        );

    final socio = UserResponse.fromJson(const {
      'id': 'user-1', 'firstName': 'Ana', 'lastName': 'Pérez', 'email': 'socio@example.com', 'status': 'ACTIVE',
    });
    final rutasDelSocio = [
      ...pantallasSinDatos.keys.where((r) => !publicRoutes.contains(r)),
      '/deposit', '/deposit/waiting', '/deposit/webview', '/payout', '/loans/applications/sol-42',
    ];

    testWidgets('sin sesión, toda pantalla del socio lleva al ingreso', (tester) async {
      await conSesion(tester, const AsyncValue.data(null));

      for (final ruta in rutasDelSocio) {
        expect(await redirigeA(ruta), '/login', reason: ruta);
      }
    });

    testWidgets('sin sesión se pueden abrir la bienvenida, el ingreso, el registro y las recuperaciones',
        (tester) async {
      await conSesion(tester, const AsyncValue.data(null));

      for (final ruta in publicRoutes) {
        expect(await redirigeA(ruta), isNull, reason: ruta);
      }
      expect(publicRoutes, {'/', '/login', '/register', '/verify-email', '/auth/recover-pin'});
    });

    testWidgets('con sesión iniciada se abre cualquier pantalla', (tester) async {
      await conSesion(tester, AsyncValue.data(socio));

      for (final ruta in [...rutasDelSocio, ...publicRoutes]) {
        expect(await redirigeA(ruta), isNull, reason: ruta);
      }
    });

    testWidgets('mientras la sesión se está cargando no saca al socio de donde está', (tester) async {
      await conSesion(tester, const AsyncValue.loading());

      expect(await redirigeA('/home'), isNull);
    });

    testWidgets('si la sesión falló al cargar, lleva al ingreso', (tester) async {
      await conSesion(tester, AsyncValue.error(Exception('401'), StackTrace.empty));

      expect(await redirigeA('/home'), '/login');
    });
  });

  testWidgets('el detalle de una solicitud de crédito toma su id de la ruta', (tester) async {
    await conContexto(tester);

    final pagina = abrir('/loans/applications/:applicationId', parametros: {'applicationId': 'sol-42'})
        as LoanApplicationDetailPage;

    expect(pagina.applicationId, 'sol-42');
  });
}

/// Sesión fija para probar la protección de rutas; el resto del notifier es el real.
class _SesionFija extends AuthNotifier {
  _SesionFija(Ref ref, AsyncValue<UserResponse?> estado) : super(ref.watch(authRepositoryProvider), ref) {
    state = estado;
  }
}
