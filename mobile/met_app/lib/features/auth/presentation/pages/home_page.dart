import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/widgets/accessible_button.dart';
import 'home_widgets/quick_actions.dart';
import 'home_widgets/_more_actions_sheet.dart';
import '../providers/auth_provider.dart';
import '../../../notifications/presentation/providers/notification_provider.dart';

import 'package:flutter_animate/flutter_animate.dart';
import '../../../home/presentation/widgets/animated_virtual_card.dart';

class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  bool _obscureBalance = false;

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authStateProvider);
    final user = authState.value;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: Stack(
        children: [
          // Background Glows
          Positioned(
            top: -100,
            left: -50,
            child: Container(
              width: 300,
              height: 300,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    const Color(0xFF53A835).withOpacity(0.15),
                    Colors.transparent,
                  ],
                ),
              ),
            ),
          ),
          Positioned(
            bottom: -50,
            right: -100,
            child: Container(
              width: 400,
              height: 400,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    const Color(0xFF9CB794).withOpacity(0.1),
                    Colors.transparent,
                  ],
                ),
              ),
            ),
          ),
          SafeArea(
            child: CustomScrollView(
              slivers: [
                _buildHeader(context, user?.fullName),
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 24.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const SizedBox(height: 16),
                        AnimatedVirtualCard(
                          obscureBalance: _obscureBalance,
                          onToggleObscure: () {
                            setState(() {
                              _obscureBalance = !_obscureBalance;
                            });
                            HapticFeedback.lightImpact();
                          },
                        ).animate().fade(duration: 500.ms).slideY(begin: 0.1, end: 0),
                        
                        const SizedBox(height: 32),
                        QuickActions(
                          onTapMore: () => MoreActionsSheet.show(context),
                        ).animate(delay: 400.ms).fade(),
                        
                        const SizedBox(height: 32),
                        Text(
                          'Servicios',
                          style:
                              Theme.of(context).textTheme.titleLarge?.copyWith(
                                    color: Theme.of(context).colorScheme.onSurface,
                                    fontWeight: FontWeight.bold,
                                  ),
                        ).animate(delay: 600.ms).fade(),
                        const SizedBox(height: 16),
                        _FeatureCard(
                          icon: Icons.calculate_outlined,
                          title: 'Simular préstamo',
                          subtitle: 'Calcula cuotas y solicita',
                          onTap: () => context.push('/loans/simulate'),
                        ).animate(delay: 700.ms).fade().slideX(begin: 0.1),

                        const SizedBox(height: 32),
                        AccessibleButton(
                          label: 'Soporte 24/7',
                          semanticLabel:
                              'Abrir chat de soporte veinticuatro siete',
                          onPressed: () {
                            context.push('/support/chat');
                          },
                        ).animate(delay: 1100.ms).fade(),
                        const SizedBox(height: 40),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
      bottomNavigationBar: _buildBottomNav(),
    );
  }

  Widget _buildHeader(BuildContext context, String? fullName) {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 16, 24, 8),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              children: [
                CircleAvatar(
                  radius: 24,
                  backgroundColor: Theme.of(context).colorScheme.onSurface.withOpacity(0.1),
                  child: Icon(Icons.person, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)),
                ),
                const SizedBox(width: 16),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Hola,',
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
                        fontSize: 14,
                      ),
                    ),
                    Text(
                      fullName?.split(' ').first ?? 'Asociado',
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.onSurface,
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ],
            ),
            Row(
              children: [
                const SizedBox(width: 4),
                Consumer(
                  builder: (context, ref, child) {
                    final unreadCountAsync = ref.watch(unreadNotificationsCountProvider);
                    final unreadCount = unreadCountAsync.valueOrNull ?? 0;

                    return Stack(
                      children: [
                        IconButton(
                          icon: Icon(Icons.notifications_none,
                              color: Theme.of(context).colorScheme.onSurface),
                          onPressed: () {
                            context.push('/notifications');
                          },
                        ),
                        if (unreadCount > 0)
                          Positioned(
                            right: 8,
                            top: 8,
                            child: Container(
                              padding: const EdgeInsets.all(4),
                              decoration: const BoxDecoration(
                                color: Colors.red,
                                shape: BoxShape.circle,
                              ),
                              child: Text(
                                unreadCount > 9 ? '9+' : unreadCount.toString(),
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 10,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                          ),
                      ],
                    );
                  },
                ),
                IconButton(
                  icon: Icon(Icons.logout,
                      color: Theme.of(context).colorScheme.onSurface),
                  onPressed: () async {
                    await ref.read(authStateProvider.notifier).logout();
                    if (context.mounted) context.go('/login');
                  },
                ),
              ],
            )
          ],
        ),
      ),
    );
  }



  Widget _buildBottomNav() {
    final location =
        GoRouter.of(context).routeInformationProvider.value.uri.toString();
    final activeIndex = _activeTabIndexFromLocation(location);

    return Container(
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainer,
        border: Border(top: BorderSide(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.1))),
      ),
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _BottomNavItem(
                icon: Icons.home,
                label: 'Inicio',
                isActive: activeIndex == 0,
                onTap: () => context.go('/home'),
              ),
              _BottomNavItem(
                icon: Icons.history,
                label: 'Movimientos',
                isActive: activeIndex == 1,
                onTap: () => context.push('/loans/applications'),
              ),
              _BottomNavItem(
                icon: Icons.credit_card,
                label: 'Inversiones',
                isActive: activeIndex == 2,
                onTap: () => context.push('/investments'),
              ),
              _BottomNavItem(
                icon: Icons.person_outline,
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
    if (location.startsWith('/movements')) return 1;
    if (location.startsWith('/cards')) return 2;
    if (location.startsWith('/profile')) return 3;
    return 0;
  }
}

class _QuickAction extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback? onTap;

  const _QuickAction({
    required this.icon,
    required this.label,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(24),
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.05),
              border: Border.all(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.1)),
            ),
            child: Icon(icon, color: Theme.of(context).colorScheme.onSurface, size: 24),
          ),
          const SizedBox(height: 8),
          Text(
            label,
            style:
                TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.8), fontSize: 13),
          ),
        ],
      ),
    );
  }
}

class _FeatureCard extends StatelessWidget {
  const _FeatureCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.onSurface.withOpacity(0.05),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.1)),
        ),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.primary.withOpacity(0.15),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(icon, color: Theme.of(context).colorScheme.primary, size: 24),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: TextStyle(
                      color: Theme.of(context).colorScheme.onSurface,
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    subtitle,
                    style: TextStyle(
                      color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6),
                      fontSize: 13,
                    ),
                  ),
                ],
              ),
            ),
            Icon(Icons.chevron_right, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.54)),
          ],
        ),
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
    final activeColor = Theme.of(context).colorScheme.primary;
    final inactiveColor = Theme.of(context).colorScheme.onSurface.withOpacity(0.45);

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 14),
        decoration: BoxDecoration(
          color: isActive
              ? activeColor.withOpacity(0.12)
              : Colors.transparent,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            AnimatedSwitcher(
              duration: const Duration(milliseconds: 200),
              child: Icon(
                icon,
                key: ValueKey(isActive),
                color: isActive ? activeColor : inactiveColor,
                size: 26,
              ),
            ),
            const SizedBox(height: 3),
            Text(
              label,
              style: TextStyle(
                color: isActive ? activeColor : inactiveColor,
                fontSize: 11,
                fontWeight: isActive ? FontWeight.w700 : FontWeight.normal,
                letterSpacing: isActive ? 0.2 : 0,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
