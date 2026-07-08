import 'package:flutter/material.dart';
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

final appRouterProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/admin/login',
    routes: [
      GoRoute(
        path: '/',
        redirect: (context, state) => '/admin/login',
      ),
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
      GoRoute(path: '/admin/reports', builder: (context, state) => const RegulatoryReportsPage()),
    ],
  );
});
