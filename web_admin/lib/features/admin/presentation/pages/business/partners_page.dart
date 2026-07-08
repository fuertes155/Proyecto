import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../providers/admin_business_provider.dart';

class PartnersPage extends ConsumerWidget {
  const PartnersPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final usersAsync = ref.watch(usersProvider);
    final primary = Theme.of(context).colorScheme.primary;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Gestión de Socios (KYC)', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
      ),
      body: usersAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(child: Text('Error: $err')),
        data: (users) {
          if (users.isEmpty) {
            return const Center(child: Text('No hay socios registrados en la base de datos central.'));
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: users.length,
            itemBuilder: (context, index) {
              final user = users[index];
              return Card(
                elevation: 0,
                color: Theme.of(context).colorScheme.surface.withOpacity(0.5),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                  side: BorderSide(color: primary.withOpacity(0.2)),
                ),
                margin: const EdgeInsets.only(bottom: 12),
                child: ListTile(
                  leading: CircleAvatar(
                    backgroundColor: primary.withOpacity(0.2),
                    child: Icon(Icons.person, color: primary),
                  ),
                  title: Text('${user.firstName} ${user.lastName}', style: const TextStyle(fontWeight: FontWeight.w600)),
                  subtitle: Text(user.email),
                  trailing: Chip(
                    label: Text(user.kycStatus, style: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold)),
                    backgroundColor: user.kycStatus == 'APPROVED' ? Colors.green.withOpacity(0.2) : Colors.orange.withOpacity(0.2),
                    side: BorderSide.none,
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
