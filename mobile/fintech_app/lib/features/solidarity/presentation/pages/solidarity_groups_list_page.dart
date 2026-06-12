import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/utils/currency_formatter.dart';
import '../../../../core/widgets/accessible_button.dart';
import '../../data/models/solidarity_models.dart';
import '../providers/solidarity_provider.dart';

class SolidarityGroupsListPage extends ConsumerStatefulWidget {
  const SolidarityGroupsListPage({super.key});

  @override
  ConsumerState<SolidarityGroupsListPage> createState() => _SolidarityGroupsListPageState();
}

class _SolidarityGroupsListPageState extends ConsumerState<SolidarityGroupsListPage> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(solidarityGroupsProvider.notifier).load());
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(solidarityGroupsProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Ahorro solidario')),
      floatingActionButton: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          FloatingActionButton.extended(
            heroTag: 'join',
            onPressed: () => context.push('/solidarity/join'),
            icon: const Icon(Icons.group_add),
            label: const Text('Unirme'),
          ),
          const SizedBox(height: 12),
          FloatingActionButton.extended(
            heroTag: 'create',
            onPressed: () => context.push('/solidarity/create'),
            icon: const Icon(Icons.add),
            label: const Text('Crear grupo'),
          ),
        ],
      ),
      body: state.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Error: $e')),
        data: (groups) {
          if (groups.isEmpty) {
            return Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.handshake_outlined, size: 72),
                  const SizedBox(height: 16),
                  const Text(
                    'Micropréstamos entre asociados',
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Crea un grupo solidario o únete con un código de invitación.',
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 24),
                  AccessibleButton(
                    label: 'Crear grupo solidario',
                    onPressed: () => context.push('/solidarity/create'),
                  ),
                ],
              ),
            );
          }
          return RefreshIndicator(
            onRefresh: () => ref.read(solidarityGroupsProvider.notifier).load(),
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: groups.length,
              itemBuilder: (_, i) => _GroupCard(group: groups[i]),
            ),
          );
        },
      ),
    );
  }
}

class _GroupCard extends StatelessWidget {
  const _GroupCard({required this.group});
  final SolidarityGroup group;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: ListTile(
        contentPadding: const EdgeInsets.all(16),
        title: Text(group.name, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 8),
            Text('Fondo: ${formatCop(group.poolBalance)}', style: const TextStyle(fontSize: 16)),
            Text('${group.memberCount}/${group.maxMembers} miembros'),
          ],
        ),
        trailing: const Icon(Icons.chevron_right),
        onTap: () => context.push('/solidarity/${group.id}'),
      ),
    );
  }
}
