import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/widgets/accessible_button.dart';
import '../providers/auth_provider.dart';

class PersonalDataPage extends ConsumerWidget {
  const PersonalDataPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authStateProvider).valueOrNull;
    if (user == null) return const Scaffold();

    return Scaffold(
      appBar: AppBar(title: const Text('Mis Datos Personales')),
      body: SingleChildScrollView(
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
              const SizedBox(height: 16),
              TextFormField(
                initialValue: user.fullName,
                decoration: const InputDecoration(
                  labelText: 'Nombre Completo',
                ),
                readOnly: true,
              ),
              const SizedBox(height: 16),
              if (user.hasEmail) ...[
                TextFormField(
                  initialValue: user.email,
                  decoration: const InputDecoration(
                    labelText: 'Correo Electrónico',
                  ),
                  readOnly: true,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  initialValue: user.phone,
                  decoration: const InputDecoration(
                    labelText: 'Número Celular',
                  ),
                  readOnly: true,
                ),
              ] else
                const _ContactDataForm(),
              const SizedBox(height: 16),
              TextFormField(
                initialValue: user.status,
                decoration: const InputDecoration(
                  labelText: 'Estado de la Cuenta',
                ),
                readOnly: true,
              ),
          ],
        ),
      ),
    );
  }
}

/// El registro solo pide cédula, nombre completo y PIN, así que una cuenta
/// nueva llega aquí sin correo ni celular. Se piden una sola vez: el correo es
/// a donde llegan los códigos de seguridad (transferencias, recargas,
/// recuperación de PIN). Una vez guardados quedan de solo lectura como el resto.
class _ContactDataForm extends ConsumerStatefulWidget {
  const _ContactDataForm();

  @override
  ConsumerState<_ContactDataForm> createState() => _ContactDataFormState();
}

class _ContactDataFormState extends ConsumerState<_ContactDataForm> {
  static final _emailPattern = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');
  static final _phonePattern = RegExp(r'^3\d{9}$');

  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController();
  bool _isSaving = false;

  @override
  void dispose() {
    _emailController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    final email = _emailController.text.trim();
    final messenger = ScaffoldMessenger.of(context);

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Confirma tu correo'),
        content: Text(
          'A $email te llegarán tus códigos de seguridad. Revisa que esté bien escrito: después solo se puede cambiar contactando a soporte.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Corregir'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Guardar'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    setState(() => _isSaving = true);
    try {
      await ref.read(authStateProvider.notifier).updateProfile(
            email: email,
            phone: _phoneController.text.trim(),
          );
      messenger.showSnackBar(
        const SnackBar(content: Text('Datos de contacto guardados.')),
      );
    } on DioException catch (e) {
      final data = e.response?.data;
      final message = data is Map ? data['message']?.toString() : null;
      messenger.showSnackBar(
        SnackBar(
          content: Text(message ?? 'No pudimos guardar tus datos. Intenta de nuevo.'),
          backgroundColor: Colors.redAccent,
        ),
      );
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Agrega tu correo y tu celular para poder transferir, recargar y recuperar tu PIN.',
            style: TextStyle(fontSize: 14, color: Colors.grey),
          ),
          const SizedBox(height: 16),
          TextFormField(
            controller: _emailController,
            decoration: const InputDecoration(labelText: 'Correo Electrónico'),
            keyboardType: TextInputType.emailAddress,
            autocorrect: false,
            validator: (v) => v == null || !_emailPattern.hasMatch(v.trim()) ? 'Correo inválido' : null,
          ),
          const SizedBox(height: 16),
          TextFormField(
            controller: _phoneController,
            decoration: const InputDecoration(labelText: 'Número Celular (10 dígitos)'),
            keyboardType: TextInputType.phone,
            inputFormatters: [FilteringTextInputFormatter.digitsOnly],
            validator: (v) => v == null || !_phonePattern.hasMatch(v) ? 'Celular inválido' : null,
          ),
          const SizedBox(height: 16),
          AccessibleButton(
            label: 'Guardar datos de contacto',
            isLoading: _isSaving,
            onPressed: _save,
          ),
        ],
      ),
    );
  }
}
