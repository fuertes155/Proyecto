import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/presentation/pages/home_page.dart';
import '../../features/auth/presentation/pages/login_page.dart';
import '../../features/auth/presentation/pages/register_page.dart';
import '../../features/auth/presentation/pages/biometric_registration_page.dart';
import '../../features/auth/presentation/pages/splash_page.dart';
import '../../features/auth/presentation/pages/email_verification_page.dart';
import '../../features/auth/presentation/pages/profile_page.dart';
import '../../features/auth/presentation/pages/personal_data_page.dart';
import '../../features/auth/presentation/pages/security_settings_page.dart';
import '../../features/auth/presentation/pages/notifications_settings_page.dart';
import '../../features/auth/presentation/pages/pin_recovery_page.dart';
import '../../features/transfers/presentation/pages/transfer_page.dart';
import '../../features/transfers/presentation/pages/deposit_page.dart';
import '../../features/transfers/presentation/pages/deposit_waiting_page.dart';
import '../../features/transfers/presentation/pages/payment_webview_page.dart';
import '../../features/loans/presentation/pages/loan_application_detail_page.dart';
import '../../features/loans/presentation/pages/loan_applications_list_page.dart';
import '../../features/loans/presentation/pages/loan_simulation_page.dart';
import '../../features/transfers/presentation/pages/account_statement_page.dart';
import '../../features/external_accounts/presentation/pages/external_bank_accounts_page.dart';
import '../../features/external_accounts/presentation/pages/add_external_bank_account_page.dart';
import '../../features/external_accounts/presentation/pages/payout_page.dart';
import '../../features/reports/presentation/pages/reports_page.dart';
// Investment Module
import '../../features/investment/presentation/pages/investment_breakdown_page.dart';
import 'package:met/features/support/presentation/pages/chat_page.dart';
import '../../features/notifications/presentation/pages/notifications_page.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/',
    routes: [
      GoRoute(path: '/', builder: (context, state) => const SplashPage()),
      GoRoute(path: '/login', builder: (context, state) => const LoginPage()),
      GoRoute(path: '/register', builder: (context, state) => const RegisterPage()),
      GoRoute(path: '/biometric-registration', builder: (context, state) => const BiometricRegistrationPage()),
      GoRoute(
        path: '/verify-email',
        builder: (context, state) {
          final extra = state.extra as Map<String, dynamic>?;
          return EmailVerificationPage(
            documentType: extra?['documentType'] as String? ?? 'DNI',
            documentNumber: extra?['documentNumber'] as String? ?? '',
          );
        },
      ),
      GoRoute(path: '/home', builder: (context, state) => const HomePage()),
      GoRoute(path: '/profile', builder: (context, state) => const ProfilePage()),
      GoRoute(path: '/profile/personal-data', builder: (context, state) => const PersonalDataPage()),
      GoRoute(path: '/profile/security', builder: (context, state) => const SecuritySettingsPage()),
      GoRoute(path: '/profile/notifications', builder: (context, state) => const NotificationsSettingsPage()),
      GoRoute(
        path: '/notifications',
        builder: (context, state) => const NotificationsPage(),
      ),
      GoRoute(
        path: '/transfer',
        builder: (context, state) => const TransferPage(),
      ),
      GoRoute(
        path: '/deposit',
        builder: (context, state) {
          final extra = state.extra as Map<String, dynamic>?;
          return DepositPage(
            method: extra?['method'] as String? ?? 'Nequi',
            bankHint: extra?['bankHint'] as String?,
          );
        },
      ),
      GoRoute(
        path: '/deposit/waiting',
        builder: (context, state) {
          final extra = state.extra as Map<String, dynamic>?;
          return DepositWaitingPage(
            method: extra?['method'] as String? ?? 'Nequi',
            amount: extra?['amount'] as double? ?? 0.0,
          );
        },
      ),
      GoRoute(
        path: '/deposit/webview',
        builder: (context, state) {
          final extra = state.extra as Map<String, dynamic>?;
          return PaymentWebViewPage(
            paymentUrl: extra?['paymentUrl'] as String? ?? '',
            method: extra?['method'] as String? ?? 'Pago',
            amount: extra?['amount'] as double? ?? 0.0,
          );
        },
      ),
      GoRoute(path: '/auth/recover-pin', builder: (context, state) => const PinRecoveryPage()),
      GoRoute(
        path: '/accounts/external',
        builder: (context, state) => const ExternalBankAccountsPage(),
      ),
      GoRoute(
        path: '/accounts/external/add',
        builder: (context, state) => const AddExternalBankAccountPage(),
      ),
      GoRoute(
        path: '/payout',
        builder: (context, state) {
          final extra = state.extra as Map<String, dynamic>?;
          return PayoutPage(preselectedAccountId: extra?['accountId'] as String?);
        },
      ),
      GoRoute(path: '/loans/simulate', builder: (context, state) => const LoanSimulationPage()),
      GoRoute(path: '/loans/applications', builder: (context, state) => const LoanApplicationsListPage()),
      GoRoute(
        path: '/loans/applications/:applicationId',
        builder: (context, state) => LoanApplicationDetailPage(
          applicationId: state.pathParameters['applicationId']!,
        ),
      ),
      // Nota: los reportes regulatorios institucionales (Supersolidaria) NO se exponen
      // en la app de socios — agregan datos de TODOS los asociados y solo pueden verlos
      // administradores (ver /v1/compliance/** en SecurityConfig). El extracto de
      // transacciones del propio socio vive en /statement.
      GoRoute(path: '/statement', builder: (context, state) => const AccountStatementPage()),
      GoRoute(path: '/reports', builder: (context, state) => const ReportsPage()),
      // ── Investment Module ──────────────────────────────────────────────────
      GoRoute(path: '/investments', builder: (context, state) => const InvestmentBreakdownPage()),

      GoRoute(
        path: '/support/chat',
        builder: (context, state) => const ChatPage(),
      ),
    ],
  );
});
