import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../core/utils/currency_formatter.dart';
import '../../data/models/auth_models.dart';
import 'home_widgets/quick_actions.dart';
import 'home_widgets/_more_actions_sheet.dart';
import '../providers/auth_provider.dart';
import '../../../notifications/presentation/providers/notification_provider.dart';

import 'package:flutter_animate/flutter_animate.dart';
import '../../../home/domain/entities/home_summary.dart';
import '../../../home/presentation/providers/home_provider.dart';
import '../../../home/presentation/widgets/animated_virtual_card.dart';

class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  bool _obscureBalance = false;

  Future<void> _refresh() async {
    ref.invalidate(unreadNotificationsCountProvider);
    await ref.read(homeProvider.notifier).refresh();
  }

  @override
  Widget build(BuildContext context) {
    final user = ref.watch(authStateProvider).valueOrNull;
    final scheme = Theme.of(context).colorScheme;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: SafeArea(
        bottom: false,
        child: RefreshIndicator(
          onRefresh: _refresh,
          color: scheme.primary,
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
            children: [
              _Header(user: user),
              const SizedBox(height: 20),
              AnimatedVirtualCard(
                obscureBalance: _obscureBalance,
                onToggleObscure: () {
                  setState(() => _obscureBalance = !_obscureBalance);
                  HapticFeedback.lightImpact();
                },
              ).animate().fadeIn(duration: 350.ms).slideY(begin: 0.06, end: 0, curve: Curves.easeOutCubic),
              const SizedBox(height: 16),
              QuickActions(
                onTapMore: () => MoreActionsSheet.show(context),
              ).animate(delay: 80.ms).fadeIn(duration: 300.ms),
              ..._notices(user),
              const SizedBox(height: 28),
              const _SectionTitle('Tus productos'),
              const SizedBox(height: 12),
              _ProductsCard(obscure: _obscureBalance).animate(delay: 140.ms).fadeIn(duration: 300.ms),
              const SizedBox(height: 28),
              _SectionTitle(
                'Movimientos recientes',
                actionLabel: 'Ver todo',
                onAction: () => context.push('/statement'),
              ),
              const SizedBox(height: 12),
              _RecentTransactionsCard(obscure: _obscureBalance).animate(delay: 200.ms).fadeIn(duration: 300.ms),
              const SizedBox(height: 28),
              const _SectionTitle('Servicios'),
              const SizedBox(height: 12),
              _SurfaceCard(
                child: Column(
                  children: [
                    _ListRow(
                      icon: Icons.calculate_outlined,
                      title: 'Simular un préstamo',
                      subtitle: 'Conoce tu cuota antes de solicitarlo',
                      onTap: () => context.push('/loans/simulate'),
                    ),
                    const _RowDivider(),
                    _ListRow(
                      icon: Icons.support_agent_rounded,
                      title: 'Soporte 24/7',
                      subtitle: 'Escríbenos por el chat',
                      onTap: () => context.push('/support/chat'),
                    ),
                  ],
                ),
              ).animate(delay: 260.ms).fadeIn(duration: 300.ms),
            ],
          ),
        ),
      ),
      bottomNavigationBar: _buildBottomNav(),
    );
  }

  /// Avisos que el socio necesita para usar la app: datos de contacto y verificación de identidad.
  List<Widget> _notices(UserResponse? user) {
    if (user == null) return const [];
    final notices = <Widget>[];
    if (!user.hasEmail) {
      notices.add(_NoticeBanner(
        icon: Icons.mark_email_unread_outlined,
        tone: _NoticeTone.warning,
        title: 'Agrega tu correo',
        message: 'Lo necesitas para transferir, recargar y recuperar tu PIN.',
        actionLabel: 'Agregar',
        onAction: () => context.push('/profile/personal-data'),
      ));
    }
    if (user.kycStatus == 'IN_REVIEW') {
      notices.add(const _NoticeBanner(
        icon: Icons.hourglass_top_rounded,
        tone: _NoticeTone.info,
        title: 'Estamos verificando tu identidad',
        message: 'Te avisaremos cuando termine. Algunas operaciones se habilitan después.',
      ));
    } else if (user.kycStatus == 'REJECTED') {
      notices.add(_NoticeBanner(
        icon: Icons.error_outline_rounded,
        tone: _NoticeTone.error,
        title: 'No pudimos verificar tu identidad',
        message: 'Escríbenos para revisar tu caso.',
        actionLabel: 'Soporte',
        onAction: () => context.push('/support/chat'),
      ));
    }
    return [
      for (final notice in notices) ...[const SizedBox(height: 12), notice],
    ];
  }

  Widget _buildBottomNav() {
    final location =
        GoRouter.of(context).routeInformationProvider.value.uri.toString();
    final activeIndex = _activeTabIndexFromLocation(location);
    final scheme = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      decoration: BoxDecoration(
        color: isDark ? scheme.surfaceContainer : Colors.white,
        border: Border(top: BorderSide(color: scheme.onSurface.withValues(alpha: 0.06))),
      ),
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
          child: Row(
            children: [
              _BottomNavItem(
                icon: Icons.home_rounded,
                label: 'Inicio',
                isActive: activeIndex == 0,
                onTap: () => context.go('/home'),
              ),
              _BottomNavItem(
                icon: Icons.receipt_long_rounded,
                label: 'Movimientos',
                isActive: activeIndex == 1,
                onTap: () => context.push('/statement'),
              ),
              _BottomNavItem(
                icon: Icons.trending_up_rounded,
                label: 'Inversiones',
                isActive: activeIndex == 2,
                onTap: () => context.push('/investments'),
              ),
              _BottomNavItem(
                icon: Icons.person_rounded,
                label: 'Perfil',
                isActive: activeIndex == 3,
                onTap: () => context.push('/profile'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  int _activeTabIndexFromLocation(String location) {
    if (location.startsWith('/home')) return 0;
    if (location.startsWith('/statement')) return 1;
    if (location.startsWith('/investments')) return 2;
    if (location.startsWith('/profile')) return 3;
    return 0;
  }
}

class _Header extends ConsumerWidget {
  const _Header({required this.user});

  final UserResponse? user;

  static String _greeting() {
    final hour = DateTime.now().hour;
    if (hour < 12) return 'Buenos días';
    if (hour < 19) return 'Buenas tardes';
    return 'Buenas noches';
  }

  static String _initials(UserResponse? user) {
    if (user == null) return '';
    final first = user.firstName.trim();
    final last = user.lastName.trim();
    return '${first.isEmpty ? '' : first[0]}${last.isEmpty ? '' : last[0]}'.toUpperCase();
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final scheme = Theme.of(context).colorScheme;
    final firstName = user?.firstName.trim().split(RegExp(r'\s+')).first;
    final unreadCount = ref.watch(unreadNotificationsCountProvider).valueOrNull ?? 0;

    return Row(
      children: [
        Semantics(
          button: true,
          label: 'Ir a mi perfil',
          child: InkWell(
            onTap: () => context.push('/profile'),
            customBorder: const CircleBorder(),
            child: CircleAvatar(
              radius: 24,
              backgroundColor: scheme.primary.withValues(alpha: 0.14),
              child: _initials(user).isEmpty
                  ? Icon(Icons.person_rounded, color: scheme.primary)
                  : Text(
                      _initials(user),
                      style: TextStyle(color: scheme.primary, fontWeight: FontWeight.w700, fontSize: 16),
                    ),
            ),
          ),
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                _greeting(),
                style: TextStyle(color: scheme.onSurface.withValues(alpha: 0.6), fontSize: 14),
              ),
              Text(
                (firstName == null || firstName.isEmpty) ? 'Asociado' : firstName,
                style: TextStyle(color: scheme.onSurface, fontSize: 21, fontWeight: FontWeight.w700),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
        _CircleIconButton(
          icon: Icons.notifications_none_rounded,
          tooltip: unreadCount > 0 ? 'Notificaciones ($unreadCount sin leer)' : 'Notificaciones',
          badgeCount: unreadCount,
          onTap: () => context.push('/notifications'),
        ),
      ],
    );
  }
}

class _CircleIconButton extends StatelessWidget {
  const _CircleIconButton({required this.icon, required this.tooltip, required this.onTap, this.badgeCount = 0});

  final IconData icon;
  final String tooltip;
  final VoidCallback onTap;
  final int badgeCount;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Tooltip(
      message: tooltip,
      child: InkResponse(
        onTap: onTap,
        radius: 26,
        child: Stack(
          clipBehavior: Clip.none,
          children: [
            Container(
              width: 46,
              height: 46,
              decoration: BoxDecoration(
                color: isDark ? scheme.surfaceContainer : Colors.white,
                shape: BoxShape.circle,
                border: Border.all(color: scheme.onSurface.withValues(alpha: 0.07)),
              ),
              child: Icon(icon, color: scheme.onSurface, size: 24),
            ),
            if (badgeCount > 0)
              Positioned(
                right: -2,
                top: -2,
                child: Container(
                  constraints: const BoxConstraints(minWidth: 20, minHeight: 20),
                  padding: const EdgeInsets.symmetric(horizontal: 5),
                  decoration: BoxDecoration(
                    color: scheme.error,
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: Theme.of(context).scaffoldBackgroundColor, width: 2),
                  ),
                  alignment: Alignment.center,
                  child: Text(
                    badgeCount > 9 ? '9+' : '$badgeCount',
                    style: const TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.w700),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.title, {this.actionLabel, this.onAction});

  final String title;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Row(
      children: [
        Expanded(
          child: Text(
            title,
            style: TextStyle(color: scheme.onSurface, fontSize: 18, fontWeight: FontWeight.w700),
          ),
        ),
        if (actionLabel != null)
          TextButton(
            onPressed: onAction,
            style: TextButton.styleFrom(
              foregroundColor: scheme.primary,
              padding: const EdgeInsets.symmetric(horizontal: 8),
              minimumSize: const Size(0, 36),
              textStyle: Theme.of(context).textTheme.labelLarge?.copyWith(fontSize: 14, fontWeight: FontWeight.w600),
            ),
            child: Text(actionLabel!),
          ),
      ],
    );
  }
}

/// Contenedor base de las secciones: blanco con sombra suave en modo claro, superficie en oscuro.
class _SurfaceCard extends StatelessWidget {
  const _SurfaceCard({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Container(
      decoration: BoxDecoration(
        color: isDark ? scheme.surfaceContainer : Colors.white,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: scheme.onSurface.withValues(alpha: isDark ? 0.08 : 0.05)),
        boxShadow: isDark
            ? null
            : [BoxShadow(color: Colors.black.withValues(alpha: 0.035), blurRadius: 12, offset: const Offset(0, 2))],
      ),
      clipBehavior: Clip.antiAlias,
      child: Material(type: MaterialType.transparency, child: child),
    );
  }
}

class _RowDivider extends StatelessWidget {
  const _RowDivider();

  @override
  Widget build(BuildContext context) {
    return Divider(
      height: 1,
      thickness: 1,
      indent: 72,
      endIndent: 16,
      color: Theme.of(context).colorScheme.onSurface.withValues(alpha: 0.06),
    );
  }
}

class _IconTile extends StatelessWidget {
  const _IconTile({required this.icon, this.color});

  final IconData icon;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final tint = color ?? scheme.primary;
    return Container(
      width: 42,
      height: 42,
      decoration: BoxDecoration(
        color: tint.withValues(alpha: isDark ? 0.18 : 0.1),
        borderRadius: BorderRadius.circular(13),
      ),
      child: Icon(icon, color: tint, size: 22),
    );
  }
}

class _ListRow extends StatelessWidget {
  const _ListRow({
    required this.icon,
    required this.title,
    required this.subtitle,
    this.trailing,
    this.onTap,
    this.iconColor,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final Widget? trailing;
  final VoidCallback? onTap;
  final Color? iconColor;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 14, 12, 14),
        child: Row(
          children: [
            _IconTile(icon: icon, color: iconColor),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: TextStyle(color: scheme.onSurface, fontSize: 15, fontWeight: FontWeight.w600),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    style: TextStyle(color: scheme.onSurface.withValues(alpha: 0.6), fontSize: 13),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            if (trailing != null) ...[const SizedBox(width: 8), trailing!],
            if (onTap != null)
              Icon(Icons.chevron_right_rounded, color: scheme.onSurface.withValues(alpha: 0.35)),
          ],
        ),
      ),
    );
  }
}

class _ProductsCard extends ConsumerWidget {
  const _ProductsCard({required this.obscure});

  final bool obscure;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final summary = ref.watch(homeProvider).valueOrNull;
    final scheme = Theme.of(context).colorScheme;

    Widget amount(double? value) => Text(
          summary == null ? '—' : (obscure ? '\$ ••••' : formatPesos(value ?? 0)),
          style: TextStyle(color: scheme.onSurface, fontSize: 15, fontWeight: FontWeight.w600),
        );

    final pendingLoans = summary?.pendingLoans ?? 0;
    return _SurfaceCard(
      child: Column(
        children: [
          _ListRow(
            icon: Icons.savings_outlined,
            title: 'Ahorro programado',
            subtitle: 'Tus metas de ahorro',
            trailing: amount(summary?.savingsTotal),
          ),
          const _RowDivider(),
          _ListRow(
            icon: Icons.trending_up_rounded,
            title: 'Inversiones',
            subtitle: 'Tu portafolio',
            trailing: amount(summary?.investmentsTotal),
            onTap: () => context.push('/investments'),
          ),
          const _RowDivider(),
          _ListRow(
            icon: Icons.request_quote_outlined,
            title: 'Créditos',
            subtitle: pendingLoans == 0
                ? 'Sin solicitudes en curso'
                : pendingLoans == 1
                    ? '1 solicitud en revisión'
                    : '$pendingLoans solicitudes en revisión',
            onTap: () => context.push('/loans/applications'),
          ),
        ],
      ),
    );
  }
}

class _RecentTransactionsCard extends ConsumerWidget {
  const _RecentTransactionsCard({required this.obscure});

  static const _maxItems = 4;
  static const _creditColor = Color(0xFF2E7D32);

  final bool obscure;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final homeState = ref.watch(homeProvider);
    final scheme = Theme.of(context).colorScheme;

    return _SurfaceCard(
      child: homeState.when(
        loading: () => const Padding(
          padding: EdgeInsets.symmetric(vertical: 28),
          child: Center(child: SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2.5))),
        ),
        error: (error, stack) => _ListRow(
          icon: Icons.refresh_rounded,
          title: 'No pudimos cargar tus movimientos',
          subtitle: 'Toca para reintentar',
          onTap: () => ref.read(homeProvider.notifier).refresh(),
        ),
        data: (summary) {
          final items = summary.recentTransactions.take(_maxItems).toList();
          if (items.isEmpty) {
            return Padding(
              padding: const EdgeInsets.symmetric(vertical: 28, horizontal: 24),
              child: Column(
                children: [
                  Icon(Icons.receipt_long_outlined, size: 36, color: scheme.onSurface.withValues(alpha: 0.3)),
                  const SizedBox(height: 10),
                  Text(
                    'Aún no tienes movimientos',
                    style: TextStyle(color: scheme.onSurface, fontSize: 15, fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Tus recargas, transferencias y retiros aparecerán aquí.',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: scheme.onSurface.withValues(alpha: 0.6), fontSize: 13),
                  ),
                ],
              ),
            );
          }
          return Column(
            children: [
              for (var i = 0; i < items.length; i++) ...[
                if (i > 0) const _RowDivider(),
                _TransactionRow(transaction: items[i], obscure: obscure),
              ],
            ],
          );
        },
      ),
    );
  }
}

class _TransactionRow extends StatelessWidget {
  const _TransactionRow({required this.transaction, required this.obscure});

  final RecentTransaction transaction;
  final bool obscure;

  static String _typeLabel(RecentTransaction t) {
    switch (t.type) {
      case 'DEPOSIT':
        return 'Recarga';
      case 'WITHDRAWAL':
        return 'Retiro';
      case 'TRANSFER':
        return t.isCredit ? 'Transferencia recibida' : 'Transferencia enviada';
      case 'EXTERNAL_PAYOUT':
        return 'Retiro a cuenta bancaria';
      case 'INVESTMENT_FUNDING':
        return 'Inversión';
      case 'LOAN_DISBURSEMENT':
        return 'Desembolso de crédito';
      default:
        return t.isCredit ? 'Ingreso' : 'Egreso';
    }
  }

  static IconData _typeIcon(RecentTransaction t) {
    switch (t.type) {
      case 'DEPOSIT':
        return Icons.add_rounded;
      case 'TRANSFER':
        return Icons.swap_horiz_rounded;
      case 'WITHDRAWAL':
      case 'EXTERNAL_PAYOUT':
        return Icons.north_east_rounded;
      case 'INVESTMENT_FUNDING':
        return Icons.trending_up_rounded;
      case 'LOAN_DISBURSEMENT':
        return Icons.request_quote_outlined;
      default:
        return t.isCredit ? Icons.south_west_rounded : Icons.north_east_rounded;
    }
  }

  static String _dateLabel(DateTime date) {
    final local = date.toLocal();
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final day = DateTime(local.year, local.month, local.day);
    final time = DateFormat('h:mm a', 'es_CO').format(local);
    if (day == today) return 'Hoy, $time';
    if (day == today.subtract(const Duration(days: 1))) return 'Ayer, $time';
    return DateFormat(local.year == now.year ? 'd MMM' : 'd MMM y', 'es_CO').format(local);
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    // El tipo ya lo dicen el ícono y el signo: el subtítulo queda solo con la fecha para que no
    // se corte en pantallas angostas.
    final description = transaction.description.trim();
    final title = description.isNotEmpty ? description : _typeLabel(transaction);
    final subtitle = _dateLabel(transaction.createdAt);
    final creditColor = isDark ? const Color(0xFF7BCB5C) : _RecentTransactionsCard._creditColor;
    final amountText = obscure
        ? '\$ ••••'
        : '${transaction.isCredit ? '+' : '−'} ${formatPesos(transaction.amount, cents: transaction.amount % 1 != 0)}';

    return _ListRow(
      icon: _typeIcon(transaction),
      iconColor: transaction.isCredit ? creditColor : scheme.onSurface.withValues(alpha: 0.7),
      title: title,
      subtitle: subtitle,
      trailing: Text(
        amountText,
        style: TextStyle(
          color: transaction.isCredit ? creditColor : scheme.onSurface,
          fontSize: 15,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}

enum _NoticeTone { info, warning, error }

class _NoticeBanner extends StatelessWidget {
  const _NoticeBanner({
    required this.icon,
    required this.tone,
    required this.title,
    required this.message,
    this.actionLabel,
    this.onAction,
  });

  final IconData icon;
  final _NoticeTone tone;
  final String title;
  final String message;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final accent = switch (tone) {
      _NoticeTone.info => isDark ? const Color(0xFF8AB4F8) : const Color(0xFF1A5FB4),
      _NoticeTone.warning => isDark ? const Color(0xFFFFC266) : const Color(0xFFB45309),
      _NoticeTone.error => scheme.error,
    };
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 14, 8, 14),
      decoration: BoxDecoration(
        color: accent.withValues(alpha: isDark ? 0.14 : 0.08),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: accent.withValues(alpha: 0.25)),
      ),
      child: Row(
        children: [
          Icon(icon, color: accent, size: 24),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: TextStyle(color: scheme.onSurface, fontSize: 14, fontWeight: FontWeight.w700)),
                const SizedBox(height: 2),
                Text(message, style: TextStyle(color: scheme.onSurface.withValues(alpha: 0.72), fontSize: 13)),
              ],
            ),
          ),
          if (actionLabel != null)
            TextButton(
              onPressed: onAction,
              style: TextButton.styleFrom(
                foregroundColor: accent,
                textStyle: Theme.of(context).textTheme.labelLarge?.copyWith(fontSize: 14, fontWeight: FontWeight.w700),
              ),
              child: Text(actionLabel!),
            ),
        ],
      ),
    );
  }
}

class _BottomNavItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool isActive;
  final VoidCallback? onTap;

  const _BottomNavItem({
    required this.icon,
    required this.label,
    this.isActive = false,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final activeColor = scheme.primary;
    final inactiveColor = scheme.onSurface.withValues(alpha: 0.5);

    return Expanded(
      child: Semantics(
        button: true,
        selected: isActive,
        label: label,
        excludeSemantics: true,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(16),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 4),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  width: 56,
                  height: 30,
                  decoration: BoxDecoration(
                    color: isActive ? activeColor.withValues(alpha: 0.14) : Colors.transparent,
                    borderRadius: BorderRadius.circular(15),
                  ),
                  child: Icon(icon, color: isActive ? activeColor : inactiveColor, size: 24),
                ),
                const SizedBox(height: 4),
                Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: isActive ? activeColor : inactiveColor,
                    fontSize: 12,
                    fontWeight: isActive ? FontWeight.w700 : FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
