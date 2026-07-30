import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../providers/admin_provider.dart';

class AdminDashboardPage extends ConsumerWidget {
  const AdminDashboardPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final adminState = ref.watch(adminAuthProvider);
    final admin = adminState.valueOrNull;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    final businessModules = [
      _AdminModule(
        icon: Icons.people_alt_rounded,
        label: 'Gestión de\nSocios (KYC)',
        route: '/admin/partners',
      ),
      _AdminModule(
        icon: Icons.account_balance_wallet_rounded,
        label: 'Cuentas y\nSaldos',
        route: '/admin/accounts',
      ),
      _AdminModule(
        icon: Icons.request_quote_rounded,
        label: 'Solicitudes\nde Crédito',
        route: '/admin/loans',
      ),
    ];

    final securityModules = [
      _AdminModule(
        icon: Icons.emergency_rounded,
        label: 'Bloqueo\nEmergencia',
        route: '/admin/emergency-lock',
      ),
      _AdminModule(
        icon: Icons.policy_rounded,
        label: 'Reglas de\nRiesgo',
        route: '/admin/risk-rules',
      ),
      _AdminModule(
        icon: Icons.attach_money_rounded,
        label: 'Límites de\nOperación',
        route: '/admin/limits',
      ),
      _AdminModule(
        icon: Icons.percent_rounded,
        label: 'Tarifas y\nComisiones',
        route: '/admin/fees',
      ),
      _AdminModule(
        icon: Icons.build_circle_rounded,
        label: 'Mantenimiento',
        route: '/admin/maintenance',
      ),
      _AdminModule(
        icon: Icons.undo_rounded,
        label: 'Reversión\nTransacción',
        route: '/admin/transaction-reversal',
      ),
      _AdminModule(
        icon: Icons.lock_reset_rounded,
        label: 'Reseteo\nCredenciales',
        route: '/admin/reset-credentials',
      ),
      _AdminModule(
        icon: Icons.assessment_rounded,
        label: 'Reportes\nRegulatorios',
        route: '/admin/reports',
      ),
      _AdminModule(
        icon: Icons.history_rounded,
        label: 'Auditoría\nLog',
        route: '/admin/audit-log',
      ),
    ];

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: Stack(
        children: [
          if (isDark)
            Container(
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  colors: [Color(0xFF0D0D0D), Color(0xFF1A0808), Color(0xFF0D0D0D)],
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                ),
              ),
            ),
          Positioned(
            top: -100, right: -80,
            child: Container(
              width: 300, height: 300,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(colors: [
                  Theme.of(context).colorScheme.primary.withOpacity(0.2),
                  Colors.transparent,
                ]),
              ),
            ),
          ),
          SafeArea(
            child: Column(
              children: [
                // Header
                Padding(
                  padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          gradient: LinearGradient(
                              colors: [Theme.of(context).colorScheme.primary, Theme.of(context).colorScheme.primary]),
                          boxShadow: [
                            BoxShadow(
                                color: Theme.of(context).colorScheme.primary.withOpacity(0.4),
                                blurRadius: 16)
                          ],
                        ),
                        child: Icon(Icons.admin_panel_settings_rounded,
                            color: Theme.of(context).colorScheme.onSurface, size: 28),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              admin?.fullName ?? 'Administrador',
                              style: TextStyle(
                                  color: Theme.of(context).colorScheme.onSurface,
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold),
                            ),
                            Text(
                              admin?.role ?? 'ADMIN',
                              style: TextStyle(
                                  color: Theme.of(context).colorScheme.primary.withOpacity(0.9),
                                  fontSize: 12,
                                  fontWeight: FontWeight.w600,
                                  letterSpacing: 1.2),
                            ),
                          ],
                        ),
                      ),

                      IconButton(
                        onPressed: () async {
                          await ref.read(adminAuthProvider.notifier).logout();
                          if (context.mounted) context.go('/login');
                        },
                        icon: Icon(Icons.logout_rounded, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)),
                        tooltip: 'Cerrar sesión',
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 8),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(12),
                    child: BackdropFilter(
                      filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                        decoration: BoxDecoration(
                          color: Theme.of(context).colorScheme.primary.withOpacity(isDark ? 0.15 : 0.05),
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(
                              color: Theme.of(context).colorScheme.primary.withOpacity(0.3)),
                        ),
                        child: Row(
                          children: [
                            Icon(Icons.warning_amber_rounded,
                                color: Theme.of(context).colorScheme.primary, size: 18),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Text(
                                'Panel de Control — Acciones con trazabilidad total',
                                style: TextStyle(
                                    color: Theme.of(context).colorScheme.onSurface.withOpacity(0.8),
                                    fontSize: 12,
                                    fontWeight: FontWeight.w500),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                Expanded(
                  child: ListView(
                    padding: const EdgeInsets.only(bottom: 20),
                    children: [
                      // Sección 1: Operaciones
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                        child: Row(
                          children: [
                            Icon(Icons.business_center_rounded, size: 20, color: Theme.of(context).colorScheme.primary),
                            const SizedBox(width: 8),
                            Text(
                              'Operaciones y Socios',
                              style: TextStyle(
                                  color: Theme.of(context).colorScheme.onSurface,
                                  fontSize: 16,
                                  fontWeight: FontWeight.w700,
                                  letterSpacing: 0.5),
                            ),
                          ],
                        ),
                      ),
                      GridView.builder(
                        padding: const EdgeInsets.fromLTRB(20, 8, 20, 16),
                        physics: const NeverScrollableScrollPhysics(),
                        shrinkWrap: true,
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 3,
                          crossAxisSpacing: 12,
                          mainAxisSpacing: 12,
                          childAspectRatio: 0.85,
                        ),
                        itemCount: businessModules.length,
                        itemBuilder: (context, i) => _ModuleCard(module: businessModules[i]),
                      ),
                      
                      const Padding(
                        padding: EdgeInsets.symmetric(horizontal: 20),
                        child: Divider(height: 24, thickness: 1, color: Colors.black12),
                      ),

                      // Sección 2: Seguridad
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                        child: Row(
                          children: [
                            Icon(Icons.security_rounded, size: 20, color: Theme.of(context).colorScheme.primary),
                            const SizedBox(width: 8),
                            Text(
                              'Seguridad y Sistema',
                              style: TextStyle(
                                  color: Theme.of(context).colorScheme.onSurface,
                                  fontSize: 16,
                                  fontWeight: FontWeight.w700,
                                  letterSpacing: 0.5),
                            ),
                          ],
                        ),
                      ),
                      GridView.builder(
                        padding: const EdgeInsets.fromLTRB(20, 8, 20, 20),
                        physics: const NeverScrollableScrollPhysics(),
                        shrinkWrap: true,
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 3,
                          crossAxisSpacing: 12,
                          mainAxisSpacing: 12,
                          childAspectRatio: 0.85,
                        ),
                        itemCount: securityModules.length,
                        itemBuilder: (context, i) => _ModuleCard(module: securityModules[i]),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _AdminModule {
  _AdminModule({
    required this.icon,
    required this.label,
    required this.route,
  });
  final IconData icon;
  final String label;
  final String route;
}

class _ModuleCard extends StatefulWidget {
  const _ModuleCard({required this.module});
  final _AdminModule module;

  @override
  State<_ModuleCard> createState() => _ModuleCardState();
}

class _ModuleCardState extends State<_ModuleCard> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final primary = Theme.of(context).colorScheme.primary;

    return GestureDetector(
      onTapDown: (_) => setState(() => _pressed = true),
      onTapUp: (_) {
        setState(() => _pressed = false);
        context.push(widget.module.route);
      },
      onTapCancel: () => setState(() => _pressed = false),
      child: AnimatedScale(
        scale: _pressed ? 0.94 : 1.0,
        duration: const Duration(milliseconds: 120),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(18),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
            child: Container(
              decoration: BoxDecoration(
                color: isDark ? Theme.of(context).colorScheme.onSurface.withOpacity(0.06) : Theme.of(context).colorScheme.onSurface.withOpacity(0.03),
                borderRadius: BorderRadius.circular(18),
                border: Border.all(
                    color: primary.withOpacity(isDark ? 0.35 : 0.2), width: 1.5),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    width: 52, height: 52,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: primary.withOpacity(isDark ? 0.2 : 0.1),
                      border: Border.all(
                          color: primary.withOpacity(isDark ? 0.5 : 0.3), width: 1.0),
                    ),
                    child: Icon(widget.module.icon,
                        color: primary, size: 26),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    widget.module.label,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                        color: Theme.of(context).colorScheme.onSurface,
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        height: 1.3),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
