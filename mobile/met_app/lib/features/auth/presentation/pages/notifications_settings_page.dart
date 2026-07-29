import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/auth_provider.dart';

class NotificationsSettingsPage extends ConsumerStatefulWidget {
  const NotificationsSettingsPage({super.key});

  @override
  ConsumerState<NotificationsSettingsPage> createState() => _NotificationsSettingsPageState();
}

class _NotificationsSettingsPageState extends ConsumerState<NotificationsSettingsPage> {
  bool _emailEnabled = true;
  bool _pushEnabled = true;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    final user = ref.read(authStateProvider).valueOrNull;
    if (user != null) {
      _emailEnabled = user.emailNotificationsEnabled;
      _pushEnabled = user.pushNotificationsEnabled;
    }
  }

  Future<void> _save() async {
    setState(() => _isLoading = true);
    try {
      await ref.read(authStateProvider.notifier).updateNotifications(
        emailEnabled: _emailEnabled,
        pushEnabled: _pushEnabled,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Preferencias actualizadas')),
        );
        Navigator.pop(context);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: $e'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Notificaciones')),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'Preferencias de contacto',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 24),
            SwitchListTile(
              title: const Text('Correos electrónicos'),
              subtitle: const Text('Recibe actualizaciones, alertas y promociones por email'),
              value: _emailEnabled,
              onChanged: (val) => setState(() => _emailEnabled = val),
            ),
            const Divider(),
            SwitchListTile(
              title: const Text('Notificaciones Push'),
              subtitle: const Text('Recibe alertas en tu dispositivo sobre tus transacciones'),
              value: _pushEnabled,
              onChanged: (val) => setState(() => _pushEnabled = val),
            ),
            const Spacer(),
            ElevatedButton(
              onPressed: _isLoading ? null : _save,
              child: _isLoading 
                  ? const CircularProgressIndicator(color: Colors.white)
                  : const Text('Guardar cambios', style: TextStyle(color: Colors.white)),
            ),
          ],
        ),
      ),
    );
  }
}
