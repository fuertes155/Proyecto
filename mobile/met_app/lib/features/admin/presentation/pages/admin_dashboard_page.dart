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

    final modules = [
      _AdminModule(
        icon: Icons.emergency_rounded,
        label: 'Bloqueo\nEmergencia',
        color: const Color(0xFF53A835),
        route: '/admin/emergency-lock',
      ),
      _AdminModule(
        icon: Icons.policy_rounded,
        label: 'Reglas de\nRiesgo',
        color: const Color(0xFF53A835),
        route: '/admin/risk-rules',
      ),
      _AdminModule(
        icon: Icons.attach_money_rounded,
        label: 'Límites de\nOperación',
        color: const Color(0xFF00A86B),
        route: '/admin/limits',
      ),
      _AdminModule(
        icon: Icons.percent_rounded,
        label: 'Tarifas y\nComisiones',
        color: const Color(0xFF0B5FFF),
        route: '/admin/fees',
      ),
      _AdminModule(
        icon: Icons.build_circle_rounded,
        label: 'Mantenimiento',
        color: const Color(0xFF9C27B0),
        route: '/admin/maintenance',
      ),
      _AdminModule(
        icon: Icons.undo_rounded,
        label: 'Reversión\nTransacción',
        color: const Color(0xFFE91E63),
        route: '/admin/transaction-reversal',
      ),
      _AdminModule(
        icon: Icons.lock_reset_rounded,
        label: 'Reseteo\nCredenciales',
        color: const Color(0xFF00BCD4),
        route: '/admin/reset-credentials',
      ),
      _AdminModule(
        icon: Icons.assessment_rounded,
        label: 'Reportes\nRegulatorios',
        color: const Color(0xFF4CAF50),
        route: '/admin/reports',
      ),
      _AdminModule(
        icon: Icons.history_rounded,
        label: 'Auditoría\nLog',
        color: const Color(0xFFFF9800),
        route: '/admin/audit-log',
      ),
    ];

    return Scaffold(
      body: Stack(
        children: [
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
                  const Color(0xFF53A835).withOpacity(0.2),
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
                          gradient: const LinearGradient(
                              colors: [Color(0xFF53A835), Color(0xFF53A835)]),
                          boxShadow: [
                            BoxShadow(
                                color: const Color(0xFF53A835).withOpacity(0.4),
                                blurRadius: 16)
                          ],
                        ),
                        child: const Icon(Icons.admin_panel_settings_rounded,
                            color: Colors.white, size: 28),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              admin?.fullName ?? 'Administrador',
                              style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold),
                            ),
                            Text(
                              admin?.role ?? 'ADMIN',
                              style: TextStyle(
                                  color: const Color(0xFF53A835).withOpacity(0.9),
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
                        icon: const Icon(Icons.logout_rounded, color: Colors.white70),
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
                          color: const Color(0xFF53A835).withOpacity(0.15),
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(
                              color: const Color(0xFF53A835).withOpacity(0.3)),
                        ),
                        child: Row(
                          children: [
                            const Icon(Icons.warning_amber_rounded,
                                color: Color(0xFF53A835), size: 18),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Text(
                                'Panel de Control — Acciones con trazabilidad total',
                                style: TextStyle(
                                    color: Colors.white.withOpacity(0.8),
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
                const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 20),
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: Text(
                      'Módulos de Gestión',
                      style: TextStyle(
                          color: Colors.white,
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 0.3),
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                // Grid de módulos
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: GridView.builder(
                      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                        crossAxisCount: 3,
                        crossAxisSpacing: 12,
                        mainAxisSpacing: 12,
                        childAspectRatio: 0.82,
                      ),
                      itemCount: modules.length,
                      itemBuilder: (context, index) {
                        return _ModuleCard(module: modules[index]);
                      },
                    ),
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
  const _AdminModule({
    required this.icon,
    required this.label,
    required this.color,
    required this.route,
  });
  final IconData icon;
  final String label;
  final Color color;
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
                color: Colors.white.withOpacity(0.06),
                borderRadius: BorderRadius.circular(18),
                border: Border.all(
                    color: const Color(0xFF53A835).withOpacity(0.35), width: 1.5),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    width: 52, height: 52,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: const Color(0xFF53A835).withOpacity(0.2),
                      border: Border.all(
                          color: const Color(0xFF53A835).withOpacity(0.5), width: 1.0),
                    ),
                    child: Icon(widget.module.icon,
                        color: const Color(0xFF53A835), size: 26),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    widget.module.label,
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                        color: Colors.white,
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
