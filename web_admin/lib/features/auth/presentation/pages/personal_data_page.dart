import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/auth_provider.dart';

class PersonalDataPage extends ConsumerWidget {
  const PersonalDataPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authStateProvider).value;
    if (user == null) return const Scaffold();

    return Scaffold(
      appBar: AppBar(title: const Text('Mis Datos Personales')),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'Información de la Cuenta',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              'Por motivos de seguridad, la actualización de datos personales debe realizarse presencialmente o contactando a soporte.',
              style: TextStyle(fontSize: 14, color: Colors.grey),
            ),
            const SizedBox(height: 24),
            TextFormField(
              initialValue: user.id,
              decoration: InputDecoration(
                labelText: 'ID de Cuenta Principal',
                suffixIcon: IconButton(
                  icon: const Icon(Icons.copy),
                  onPressed: () {
                    Clipboard.setData(ClipboardData(text: user.id));
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('ID copiado al portapapeles')),
                    );
                  },
                ),
              ),
              readOnly: true,
            ),
          ],
        ),
      ),
    );
  }
}
