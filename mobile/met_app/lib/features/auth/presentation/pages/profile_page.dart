import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/auth_provider.dart';
import 'package:go_router/go_router.dart';

class ProfilePage extends ConsumerWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authStateProvider);
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: const Text('Mi Perfil', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        centerTitle: true,
      ),
      body: authState.when(
        loading: () => Center(child: CircularProgressIndicator(color: primaryColor)),
        error: (err, _) => Center(child: Text('Error al cargar el perfil: $err')),
        data: (user) {
          if (user == null) return const Center(child: Text('No hay sesión activa'));
          
          return ListView(
            padding: const EdgeInsets.all(24),
            children: [
              // Avatar
              Center(
                child: Container(
                  width: 100,
                  height: 100,
                  decoration: BoxDecoration(
                    color: primaryColor.withOpacity(0.1),
                    shape: BoxShape.circle,
                    border: Border.all(color: primaryColor.withOpacity(0.3), width: 2),
                  ),
                  child: Icon(Icons.person, size: 50, color: primaryColor),
                ),
              ),
              const SizedBox(height: 24),
              
              // Datos del usuario
              Text(
                '${user.firstName} ${user.lastName}',
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text(
                user.email,
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 16,
                  color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6),
                ),
              ),
              
              const SizedBox(height: 40),
              
              _ProfileOption(
                icon: Icons.person_outline,
                title: 'Mis Datos Personales',
                onTap: () => context.push('/profile/personal-data'),
              ),
              _ProfileOption(
                icon: Icons.lock_outline,
                title: 'Seguridad y Contraseña',
                onTap: () => context.push('/profile/security'),
              ),
              _ProfileOption(
                icon: Icons.notifications_none,
                title: 'Notificaciones',
                onTap: () => context.push('/profile/notifications'),
              ),
              _ProfileOption(
                icon: Icons.help_outline,
                title: 'Ayuda y Soporte',
                onTap: () => context.push('/support/chat'),
              ),
              
              const SizedBox(height: 30),
              
              // Botón de Cerrar Sesión
              ElevatedButton.icon(
                icon: const Icon(Icons.logout, color: Colors.white),
                label: const Text('Cerrar Sesión', style: TextStyle(color: Colors.white, fontSize: 16)),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.redAccent,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                ),
                onPressed: () {
                  ref.read(authStateProvider.notifier).logout();
                },
              ),
            ],
          );
        },
      ),
    );
  }
}

class _ProfileOption extends StatelessWidget {
  final IconData icon;
  final String title;
  final VoidCallback onTap;

  const _ProfileOption({
    required this.icon,
    required this.title,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.05)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(isDark ? 0.2 : 0.03),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: ListTile(
        leading: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.primary.withOpacity(0.1),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(icon, color: Theme.of(context).colorScheme.primary),
        ),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.w500)),
        trailing: Icon(Icons.chevron_right, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4)),
        onTap: onTap,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),
    );
  }
}
