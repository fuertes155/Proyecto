import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/ui/ui.dart';
import '../../providers/admin_business_provider.dart';
import '../providers/admin_provider.dart';
import '../providers/dashboard_provider.dart';
import '../shell/admin_nav.dart';

/// Inicio del panel: indicadores clave, pendientes y accesos rápidos.
class AdminDashboardPage extends ConsumerWidget {
  const AdminDashboardPage({super.key});

  static const _quickAccess = [
    partnersItem,
    loansItem,
    complianceItem,
    accountsItem,
    emergencyLockItem,
    reportsItem,
    feesItem,
    auditLogItem,
  ];

  void _refresh(WidgetRef ref) {
    ref.invalidate(usersProvider);
    ref.invalidate(accountsProvider);
    ref.invalidate(loansProvider);
    ref.invalidate(openAlertsCountProvider);
    ref.read(guaranteeFundProvider.notifier).load();
    ref.read(maintenanceProvider.notifier).load();
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final admin = ref.watch(adminAuthProvider).valueOrNull;
    final firstName = (admin?.fullName ?? '').trim().split(' ').first;

    return AdminPage(
      section: _todayLabel(),
      title: firstName.isEmpty ? _greeting() : '${_greeting()}, $firstName',
      subtitle: 'Este es el estado de la cooperativa en este momento.',
      actions: [
        OutlinedButton.icon(
          onPressed: () => _refresh(ref),
          icon: const Icon(Icons.refresh_rounded, size: 18),
          label: const Text('Actualizar'),
        ),
      ],
      children: [
        const _HeroBanner(),
        const SizedBox(height: 24),
        const _KpiGrid(),
        const SizedBox(height: 24),
        LayoutBuilder(builder: (context, c) {
          const attention = _AttentionCard();
          const quick = _QuickAccessCard(items: _quickAccess);
          if (c.maxWidth < 980) {
            return const Column(children: [attention, SizedBox(height: 20), quick]);
          }
          return const Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(flex: 5, child: attention),
              SizedBox(width: 20),
              Expanded(flex: 7, child: quick),
            ],
          );
        }),
      ],
    );
  }

  static String _greeting() {
    final hour = DateTime.now().hour;
    if (hour < 12) return 'Buenos días';
    if (hour < 19) return 'Buenas tardes';
    return 'Buenas noches';
  }

  static String _todayLabel() {
    const days = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo'];
    const months = [
      'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
      'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
    ];
    final now = DateTime.now();
    return '${days[now.weekday - 1]} ${now.day} de ${months[now.month - 1]}';
  }
}

class _HeroBanner extends StatelessWidget {
  const _HeroBanner();

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: MetColors.brandGradient,
        borderRadius: BorderRadius.circular(20),
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        children: [
          Positioned(right: -60, top: -80, child: _glow(260)),
          Positioned(right: 140, bottom: -120, child: _glow(220)),
          Padding(
            padding: const EdgeInsets.fromLTRB(28, 28, 28, 28),
            child: LayoutBuilder(builder: (context, c) {
              final wide = c.maxWidth >= 760;
              final content = Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('MET · PANEL ADMINISTRATIVO',
                      style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.65),
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 1.2)),
                  const SizedBox(height: 10),
                  Text(kMetSlogan,
                      style: TextStyle(
                          color: Colors.white,
                          fontSize: wide ? 28 : 23,
                          height: 1.2,
                          fontWeight: FontWeight.w800,
                          letterSpacing: -0.5)),
                  const SizedBox(height: 10),
                  Text(
                    'Cada validación, aprobación y alerta que atiendes aquí mantiene el dinero de los socios en movimiento y protegido.',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.82), fontSize: 15, height: 1.5),
                  ),
                  const SizedBox(height: 20),
                  Wrap(
                    spacing: 10,
                    runSpacing: 10,
                    children: [
                      FilledButton.icon(
                        onPressed: () => context.go(partnersItem.route),
                        style: FilledButton.styleFrom(
                          backgroundColor: Colors.white,
                          foregroundColor: MetColors.primary,
                        ),
                        icon: const Icon(Icons.how_to_reg_rounded, size: 18),
                        label: const Text('Revisar KYC pendientes'),
                      ),
                      OutlinedButton.icon(
                        onPressed: () => context.go(complianceItem.route),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: Colors.white,
                          side: BorderSide(color: Colors.white.withValues(alpha: 0.45)),
                        ),
                        icon: const Icon(Icons.shield_outlined, size: 18),
                        label: const Text('Ver alertas SARLAFT'),
                      ),
                    ],
                  ),
                ],
              );
              if (!wide) return content;
              return Row(
                children: [
                  Expanded(child: content),
                  const SizedBox(width: 32),
                  const _HeroPillars(),
                ],
              );
            }),
          ),
        ],
      ),
    );
  }

  static Widget _glow(double size) => Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          gradient: RadialGradient(colors: [
            const Color(0xFF8BC34A).withValues(alpha: 0.18),
            Colors.transparent,
          ]),
        ),
      );
}

/// Los dos pilares del eslogan como tarjetas sobre el banner.
class _HeroPillars extends StatelessWidget {
  const _HeroPillars();

  @override
  Widget build(BuildContext context) {
    Widget pillar(IconData icon, String title, String text) => Container(
          width: 250,
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: Colors.white.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: Colors.white.withValues(alpha: 0.16)),
          ),
          child: Row(
            children: [
              Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  color: MetColors.accent.withValues(alpha: 0.9),
                  borderRadius: BorderRadius.circular(11),
                ),
                child: Icon(icon, color: Colors.white, size: 20),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title,
                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 14.5)),
                    Text(text,
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.72), fontSize: 12.5, height: 1.35)),
                  ],
                ),
              ),
            ],
          ),
        );
    return Column(
      children: [
        pillar(Icons.bolt_rounded, 'Impulso instantáneo', 'Decisiones ágiles para que el dinero no espere.'),
        const SizedBox(height: 12),
        pillar(Icons.verified_user_rounded, 'Confianza permanente', 'Controles y auditoría en cada acción.'),
      ],
    );
  }
}

class _KpiGrid extends ConsumerWidget {
  const _KpiGrid();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final users = ref.watch(usersProvider);
    final accounts = ref.watch(accountsProvider);
    final loans = ref.watch(loansProvider);
    final fund = ref.watch(guaranteeFundProvider);

    final pendingKyc = users.valueOrNull?.where((u) => isKycPending(u.kycStatus)).length ?? 0;
    final activeAccounts = accounts.valueOrNull?.where((a) => a.status == 'ACTIVE').length ?? 0;
    final totalBalance = accounts.valueOrNull?.fold<double>(0, (sum, a) => sum + a.balance) ?? 0;
    final awaitingLoans = loans.valueOrNull?.where((l) => isLoanAwaitingDecision(l.status)).length ?? 0;

    return ResponsiveGrid(
      minItemWidth: 160,
      children: [
        StatCard(
          icon: Icons.people_alt_rounded,
          label: 'Socios registrados',
          value: '${users.valueOrNull?.length ?? 0}',
          hint: pendingKyc == 0 ? 'Todos con KYC resuelto' : '$pendingKyc con KYC pendiente',
          loading: users.isLoading,
          failed: users.hasError,
          onTap: () => context.go(partnersItem.route),
        ),
        StatCard(
          icon: Icons.account_balance_wallet_rounded,
          label: 'Saldo total en cuentas',
          value: formatCop(totalBalance),
          hint: '$activeAccounts cuentas activas',
          loading: accounts.isLoading,
          failed: accounts.hasError,
          onTap: () => context.go(accountsItem.route),
        ),
        StatCard(
          icon: Icons.request_quote_rounded,
          label: 'Créditos por decidir',
          value: '$awaitingLoans',
          hint: '${loans.valueOrNull?.length ?? 0} solicitudes en total',
          tone: awaitingLoans > 0 ? Tone.warning : Tone.success,
          loading: loans.isLoading,
          failed: loans.hasError,
          onTap: () => context.go(loansItem.route),
        ),
        StatCard(
          icon: Icons.savings_rounded,
          label: 'Fondo de garantías',
          value: fund.valueOrNull == null ? null : formatCop(fund.valueOrNull!.balance),
          hint: fund.valueOrNull == null
              ? null
              : 'Cubre hasta el ${(fund.valueOrNull!.coverageRatio * 100).toStringAsFixed(0)}% en mora',
          tone: Tone.info,
          loading: fund.isLoading,
          failed: fund.hasError,
          onTap: () => context.go(guaranteeFundItem.route),
        ),
      ],
    );
  }
}

class _AttentionCard extends ConsumerWidget {
  const _AttentionCard();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final users = ref.watch(usersProvider);
    final loans = ref.watch(loansProvider);
    final alerts = ref.watch(openAlertsCountProvider);
    final maintenance = ref.watch(maintenanceProvider);

    final pendingKyc = users.valueOrNull?.where((u) => isKycPending(u.kycStatus)).length;
    final awaitingLoans = loans.valueOrNull?.where((l) => isLoanAwaitingDecision(l.status)).length;
    final openAlerts = alerts.valueOrNull;
    final activeWindows = maintenance.valueOrNull?.where((w) => w.activo).length;

    final rows = <Widget>[
      _AttentionRow(
        icon: Icons.how_to_reg_rounded,
        title: 'Validaciones KYC',
        subtitle: 'Socios esperando aprobación de identidad',
        count: pendingKyc,
        loading: users.isLoading,
        tone: Tone.warning,
        onTap: () => context.go(partnersItem.route),
      ),
      _AttentionRow(
        icon: Icons.request_quote_rounded,
        title: 'Solicitudes de crédito',
        subtitle: 'Nuevas o en revisión',
        count: awaitingLoans,
        loading: loans.isLoading,
        tone: Tone.warning,
        onTap: () => context.go(loansItem.route),
      ),
      _AttentionRow(
        icon: Icons.shield_rounded,
        title: 'Alertas SARLAFT abiertas',
        subtitle: 'Operaciones inusuales sin revisar',
        count: openAlerts,
        loading: alerts.isLoading,
        tone: Tone.danger,
        onTap: () => context.go(complianceItem.route),
      ),
      _AttentionRow(
        icon: Icons.build_circle_rounded,
        title: 'Mantenimiento activo',
        subtitle: 'Ventanas que afectan a los socios ahora',
        count: activeWindows,
        loading: maintenance.isLoading,
        tone: Tone.info,
        onTap: () => context.go(maintenanceItem.route),
      ),
    ];

    final allClear = !users.isLoading &&
        !loans.isLoading &&
        !alerts.isLoading &&
        (pendingKyc ?? 0) == 0 &&
        (awaitingLoans ?? 0) == 0 &&
        (openAlerts ?? 0) == 0 &&
        (activeWindows ?? 0) == 0 &&
        !users.hasError &&
        !loans.hasError &&
        !alerts.hasError;

    return SectionCard(
      title: 'Requiere tu atención',
      subtitle: allClear ? 'No hay pendientes en este momento.' : 'Tareas que esperan una decisión.',
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 12),
      child: Column(
        children: [
          for (var i = 0; i < rows.length; i++) ...[
            rows[i],
            if (i < rows.length - 1) const Divider(),
          ],
        ],
      ),
    );
  }
}

class _AttentionRow extends StatelessWidget {
  const _AttentionRow({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.count,
    required this.loading,
    required this.tone,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final int? count;
  final bool loading;
  final Tone tone;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final hasItems = (count ?? 0) > 0;
    Widget trailing;
    if (loading) {
      trailing = const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2));
    } else if (count == null) {
      trailing = const Text('—', style: TextStyle(color: MetColors.textFaint));
    } else if (hasItems) {
      trailing = Container(
        constraints: const BoxConstraints(minWidth: 30),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(color: tone.background, borderRadius: BorderRadius.circular(999)),
        child: Text('$count',
            textAlign: TextAlign.center,
            style: TextStyle(color: tone.foreground, fontWeight: FontWeight.w800, fontSize: 14)),
      );
    } else {
      trailing = const Icon(Icons.check_circle_rounded, color: MetColors.success, size: 22);
    }

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      hoverColor: MetColors.subtle,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 4),
        child: Row(
          children: [
            IconBadge(icon, tone: hasItems ? tone : Tone.neutral, size: 40),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: const TextStyle(fontSize: 14.5, fontWeight: FontWeight.w700)),
                  Text(subtitle, style: const TextStyle(fontSize: 13, color: MetColors.textMuted)),
                ],
              ),
            ),
            const SizedBox(width: 12),
            trailing,
            const SizedBox(width: 6),
            const Icon(Icons.chevron_right_rounded, color: MetColors.textFaint),
          ],
        ),
      ),
    );
  }
}

class _QuickAccessCard extends StatelessWidget {
  const _QuickAccessCard({required this.items});

  final List<AdminNavItem> items;

  @override
  Widget build(BuildContext context) {
    return SectionCard(
      title: 'Accesos rápidos',
      subtitle: 'Las tareas más frecuentes del panel.',
      child: ResponsiveGrid(
        minItemWidth: 200,
        spacing: 12,
        maxColumns: 2,
        children: [for (final item in items) _QuickTile(item: item)],
      ),
    );
  }
}

class _QuickTile extends StatelessWidget {
  const _QuickTile({required this.item});

  final AdminNavItem item;

  @override
  Widget build(BuildContext context) {
    final critical = item.route == emergencyLockItem.route;
    return Material(
      color: MetColors.subtle,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        onTap: () => context.go(item.route),
        borderRadius: BorderRadius.circular(14),
        hoverColor: MetColors.successBg,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              IconBadge(item.icon, tone: critical ? Tone.danger : Tone.success, size: 40),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(item.label,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 14.5, fontWeight: FontWeight.w700)),
                    Text(item.description,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 12.5, color: MetColors.textMuted)),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
