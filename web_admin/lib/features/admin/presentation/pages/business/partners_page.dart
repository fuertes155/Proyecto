import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../providers/admin_business_provider.dart';

String _friendlyError(Object e) {
  if (e is DioException) {
    final status = e.response?.statusCode;
    if (status == 401) return 'Tu sesión de administrador expiró. Vuelve a iniciar sesión.';
    if (status == 403) return 'No tienes permisos para esta acción.';
    final data = e.response?.data;
    if (data is Map && data['message'] is String) return data['message'] as String;
    return 'No se pudo completar la acción${status != null ? ' (error $status)' : ''}.';
  }
  return 'Ocurrió un error inesperado.';
}

Future<void> _reviewKyc({
  required BuildContext context,
  required WidgetRef ref,
  required UserModel user,
  required String targetStatus, // 'APPROVED' | 'REJECTED'
}) async {
  final isApprove = targetStatus == 'APPROVED';
  final confirmed = await showDialog<bool>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: Text(isApprove ? 'Aprobar KYC' : 'Denegar KYC'),
      content: Text(
        isApprove
            ? '¿Confirmas que "${user.firstName} ${user.lastName}" (CC ${user.documentNumber}) pasó la validación de identidad?'
            : '¿Confirmas que vas a denegar el KYC de "${user.firstName} ${user.lastName}" (CC ${user.documentNumber})? '
              'No podrá depositar ni operar hasta que se apruebe.',
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')),
        FilledButton(
          onPressed: () => Navigator.pop(ctx, true),
          style: isApprove
              ? null
              : FilledButton.styleFrom(backgroundColor: Colors.red.shade700),
          child: Text(isApprove ? 'Aprobar' : 'Denegar'),
        ),
      ],
    ),
  );
  if (confirmed != true) return;

  final dio = ref.read(adminApiClientProvider);
  try {
    await dio.put('/users/${user.id}/kyc?status=$targetStatus');
    ref.invalidate(usersProvider);
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(isApprove ? 'KYC de ${user.firstName} aprobado' : 'KYC de ${user.firstName} denegado'),
        backgroundColor: isApprove ? Colors.green.shade700 : Colors.red.shade700,
      ));
    }
  } catch (e) {
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(_friendlyError(e)),
        backgroundColor: Colors.red.shade700,
      ));
    }
  }
}

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
              final isApproved = user.kycStatus == 'APPROVED';
              final isRejected = user.kycStatus == 'REJECTED';
              final chipColor = isApproved
                  ? Colors.green.withOpacity(0.2)
                  : (isRejected ? Colors.red.withOpacity(0.2) : Colors.orange.withOpacity(0.2));
              return Card(
                elevation: 0,
                color: Theme.of(context).colorScheme.surface.withOpacity(0.5),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                  side: BorderSide(color: primary.withOpacity(0.2)),
                ),
                margin: const EdgeInsets.only(bottom: 12),
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 14, 16, 14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          CircleAvatar(
                            backgroundColor: primary.withOpacity(0.2),
                            child: Icon(Icons.person, color: primary),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text('${user.firstName} ${user.lastName}',
                                    style: const TextStyle(fontWeight: FontWeight.w600)),
                                const SizedBox(height: 2),
                                Text(user.email, style: Theme.of(context).textTheme.bodySmall),
                                Text('CC: ${user.documentNumber}', style: Theme.of(context).textTheme.bodySmall),
                              ],
                            ),
                          ),
                          const SizedBox(width: 8),
                          Chip(
                            label: Text(user.kycStatus, style: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold)),
                            backgroundColor: chipColor,
                            side: BorderSide.none,
                            visualDensity: VisualDensity.compact,
                            materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          ),
                        ],
                      ),
                      if (!isApproved) ...[
                        const SizedBox(height: 12),
                        Divider(height: 1, color: primary.withOpacity(0.12)),
                        const SizedBox(height: 10),
                        Align(
                          alignment: Alignment.centerRight,
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              if (!isRejected) ...[
                                TextButton(
                                  onPressed: () => _reviewKyc(
                                    context: context, ref: ref, user: user, targetStatus: 'REJECTED',
                                  ),
                                  style: TextButton.styleFrom(
                                    foregroundColor: Theme.of(context).colorScheme.error,
                                    minimumSize: Size.zero,
                                    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                                    textStyle: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500),
                                  ),
                                  child: const Text('Denegar'),
                                ),
                                const SizedBox(width: 4),
                              ],
                              FilledButton(
                                onPressed: () => _reviewKyc(
                                  context: context, ref: ref, user: user, targetStatus: 'APPROVED',
                                ),
                                style: FilledButton.styleFrom(
                                  backgroundColor: primary,
                                  foregroundColor: Colors.white,
                                  elevation: 0,
                                  minimumSize: Size.zero,
                                  tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                                  textStyle: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
                                ),
                                child: const Text('Aprobar'),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ],
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
