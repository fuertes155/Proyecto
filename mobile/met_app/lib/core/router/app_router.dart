import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/presentation/pages/home_page.dart';
import '../../features/auth/presentation/pages/login_page.dart';
import '../../features/auth/presentation/pages/register_page.dart';
import '../../features/auth/presentation/pages/splash_page.dart';
import '../../features/auth/presentation/pages/profile_page.dart';
import '../../features/auth/presentation/pages/personal_data_page.dart';
import '../../features/auth/presentation/pages/security_settings_page.dart';
import '../../features/auth/presentation/pages/notifications_settings_page.dart';
import '../../features/auth/presentation/pages/pin_recovery_page.dart';
import '../../features/savings/presentation/pages/create_scheduled_savings_page.dart';
import '../../features/savings/presentation/pages/scheduled_savings_detail_page.dart';
import '../../features/savings/presentation/pages/scheduled_savings_list_page.dart';
import '../../features/transfers/presentation/pages/transfer_page.dart';
import '../../features/transfers/presentation/pages/deposit_page.dart';
import '../../features/solidarity/presentation/pages/create_solidarity_group_page.dart';
import '../../features/solidarity/presentation/pages/join_solidarity_group_page.dart';
import '../../features/solidarity/presentation/pages/solidarity_group_detail_page.dart';
import '../../features/solidarity/presentation/pages/solidarity_groups_list_page.dart';
import '../../features/loans/presentation/pages/loan_application_detail_page.dart';
import '../../features/loans/presentation/pages/loan_applications_list_page.dart';
import '../../features/loans/presentation/pages/loan_simulation_page.dart';
import '../../features/compliance/presentation/pages/regulatory_reports_page.dart';
// Investment Module
import '../../features/investment/presentation/pages/investment_home_page.dart';
import '../../features/investment/presentation/pages/create_portfolio_page.dart';
import '../../features/investment/presentation/pages/portfolio_detail_page.dart';
// Admin Module
import '../../features/admin/presentation/pages/admin_login_page.dart';
import '../../features/admin/presentation/pages/admin_dashboard_page.dart';
import '../../features/admin/presentation/pages/emergency_lock_page.dart';
import '../../features/admin/presentation/pages/operation_limits_page.dart';
import '../../features/admin/presentation/pages/audit_log_page.dart';
import '../../features/admin/presentation/pages/maintenance_page.dart';
import '../../features/admin/presentation/pages/fees_page.dart';
import '../../features/admin/presentation/pages/risk_rules_page.dart';
import '../../features/admin/presentation/pages/transaction_reversal_page.dart';
import '../../features/admin/presentation/pages/reset_credentials_page.dart';
import 'package:met/features/support/presentation/pages/chat_page.dart';
import '../../features/notifications/presentation/pages/notifications_page.dart';
import '../../features/transfers/presentation/pages/transfer_page.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/',
    routes: [
      GoRoute(path: '/', builder: (context, state) => const SplashPage()),
      GoRoute(path: '/login', builder: (context, state) => const LoginPage()),
      GoRoute(path: '/register', builder: (context, state) => const RegisterPage()),
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
          return DepositPage(method: extra?['method'] as String? ?? 'Nequi');
        },
      ),
      GoRoute(path: '/auth/recover-pin', builder: (context, state) => const PinRecoveryPage()),
      GoRoute(
        path: '/savings/scheduled',
        builder: (context, state) => const ScheduledSavingsListPage(),
      ),
      GoRoute(
        path: '/savings/scheduled/create',
        builder: (context, state) => const CreateScheduledSavingsPage(),
      ),
      GoRoute(
        path: '/savings/scheduled/:accountId',
        builder: (context, state) => ScheduledSavingsDetailPage(
          accountId: state.pathParameters['accountId']!,
        ),
      ),
      GoRoute(path: '/solidarity', builder: (context, state) => const SolidarityGroupsListPage()),
      GoRoute(path: '/solidarity/create', builder: (context, state) => const CreateSolidarityGroupPage()),
      GoRoute(path: '/solidarity/join', builder: (context, state) => const JoinSolidarityGroupPage()),
      GoRoute(
        path: '/solidarity/:groupId',
        builder: (context, state) => SolidarityGroupDetailPage(
          groupId: state.pathParameters['groupId']!,
        ),
      ),
      GoRoute(path: '/loans/simulate', builder: (context, state) => const LoanSimulationPage()),
      GoRoute(path: '/loans/applications', builder: (context, state) => const LoanApplicationsListPage()),
      GoRoute(
        path: '/loans/applications/:applicationId',
        builder: (context, state) => LoanApplicationDetailPage(
          applicationId: state.pathParameters['applicationId']!,
        ),
      ),
      GoRoute(path: '/compliance/reports', builder: (context, state) => const RegulatoryReportsPage()),
      GoRoute(path: '/admin/reports', builder: (context, state) => const RegulatoryReportsPage()),
      // ── Investment Module ──────────────────────────────────────────────────
      GoRoute(path: '/investments', builder: (context, state) => const InvestmentHomePage()),
      GoRoute(path: '/investments/create', builder: (context, state) => const CreatePortfolioPage()),
      GoRoute(
        path: '/investments/portfolio/:portfolioId',
        builder: (context, state) => PortfolioDetailPage(
          portfolioId: state.pathParameters['portfolioId']!,
        ),
      ),
      // ── Admin Module ──────────────────────────────────────────────────────
      GoRoute(path: '/admin/login', builder: (context, state) => const AdminLoginPage()),
      GoRoute(path: '/admin/dashboard', builder: (context, state) => const AdminDashboardPage()),
      GoRoute(path: '/admin/emergency-lock', builder: (context, state) => const EmergencyLockPage()),
      GoRoute(path: '/admin/maintenance', builder: (context, state) => const MaintenancePage()),
      GoRoute(path: '/admin/fees', builder: (context, state) => const FeesPage()),
      GoRoute(path: '/admin/risk-rules', builder: (context, state) => const RiskRulesPage()),
      GoRoute(path: '/admin/transaction-reversal', builder: (context, state) => const TransactionReversalPage()),
      GoRoute(path: '/admin/reset-credentials', builder: (context, state) => const ResetCredentialsPage()),
      GoRoute(path: '/admin/limits', builder: (context, state) => const OperationLimitsPage()),
      GoRoute(path: '/admin/audit-log', builder: (context, state) => const AuditLogPage()),
      GoRoute(
        path: '/support/chat',
        builder: (context, state) => const ChatPage(),
      ),
    ],
  );
});
