import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/widgets/accessible_button.dart';
import '../providers/auth_provider.dart';

class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authStateProvider);
    final user = authState.value;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Inicio'),
        actions: [
          IconButton(
            tooltip: 'Cerrar sesión',
            onPressed: () async {
              await ref.read(authStateProvider.notifier).logout();
              if (context.mounted) context.go('/login');
            },
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Hola, ${user?.fullName ?? 'Asociado'}',
              style: Theme.of(context).textTheme.headlineLarge,
            ),
            const SizedBox(height: 8),
            Text(user?.email ?? '', style: Theme.of(context).textTheme.bodyLarge),
            const SizedBox(height: 32),
            _FeatureCard(
              icon: Icons.savings,
              title: 'Ahorro programado',
              subtitle: 'Configura aportes automáticos',
              onTap: () => context.push('/savings/scheduled'),
            ),
            const SizedBox(height: 16),
            _FeatureCard(
              icon: Icons.handshake,
              title: 'Ahorro solidario',
              subtitle: 'Micropréstamos entre asociados',
              onTap: () => context.push('/solidarity'),
            ),
            const SizedBox(height: 16),
            _FeatureCard(
              icon: Icons.calculate,
              title: 'Simular préstamo',
              subtitle: 'Calcula cuotas y solicita',
              onTap: () => context.push('/loans/simulate'),
            ),
            const SizedBox(height: 16),
            _FeatureCard(
              icon: Icons.description_outlined,
              title: 'Reportes Supersolidaria',
              subtitle: 'Archivos planos regulatorios',
              onTap: () => context.push('/compliance/reports'),
            ),
            const Spacer(),
            AccessibleButton(
              label: 'Soporte 24/7',
              semanticLabel: 'Abrir chat de soporte veinticuatro siete',
              onPressed: () {},
            ),
          ],
        ),
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
    return Card(
      elevation: 2,
      child: ListTile(
        leading: Icon(icon, size: 32),
        title: Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
        subtitle: Text(subtitle, style: const TextStyle(fontSize: 16)),
        trailing: const Icon(Icons.chevron_right),
        onTap: onTap,
      ),
    );
  }
}
