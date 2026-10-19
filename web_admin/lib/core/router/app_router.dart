import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

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
import '../../features/admin/presentation/pages/regulatory_reports_page.dart';
import '../../features/admin/presentation/pages/compliance_page.dart';
import '../../features/admin/presentation/pages/business/partners_page.dart';
import '../../features/admin/presentation/pages/business/accounts_page.dart';
import '../../features/admin/presentation/pages/business/loans_page.dart';
import '../../features/admin/presentation/pages/guarantee_fund_page.dart';
import '../../features/admin/presentation/shell/admin_shell.dart';

/// Páginas dentro del panel (con menú lateral). Sin animación entre secciones:
/// en un panel web el cambio debe sentirse inmediato.
GoRoute _panelRoute(String path, Widget page) => GoRoute(
      path: path,
      pageBuilder: (context, state) => NoTransitionPage(key: state.pageKey, child: page),
    );

final appRouterProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/admin/login',
    routes: [
      GoRoute(path: '/', redirect: (context, state) => '/admin/login'),
      GoRoute(path: '/admin', redirect: (context, state) => '/admin/dashboard'),
      GoRoute(path: '/admin/login', builder: (context, state) => const AdminLoginPage()),
      ShellRoute(
        builder: (context, state, child) => AdminShell(location: state.uri.path, child: child),
        routes: [
          _panelRoute('/admin/dashboard', const AdminDashboardPage()),
          _panelRoute('/admin/partners', const PartnersPage()),
          _panelRoute('/admin/accounts', const AccountsPage()),
          _panelRoute('/admin/loans', const LoansPage()),
          _panelRoute('/admin/compliance', const CompliancePage()),
          _panelRoute('/admin/risk-rules', const RiskRulesPage()),
          _panelRoute('/admin/guarantee-fund', const GuaranteeFundPage()),
          _panelRoute('/admin/limits', const OperationLimitsPage()),
          _panelRoute('/admin/fees', const FeesPage()),
          _panelRoute('/admin/maintenance', const MaintenancePage()),
          _panelRoute('/admin/emergency-lock', const EmergencyLockPage()),
          _panelRoute('/admin/transaction-reversal', const TransactionReversalPage()),
          _panelRoute('/admin/reset-credentials', const ResetCredentialsPage()),
          _panelRoute('/admin/reports', const RegulatoryReportsPage()),
          _panelRoute('/admin/audit-log', const AuditLogPage()),
        ],
      ),
    ],
  );
});
