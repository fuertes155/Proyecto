import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/widgets/accessible_button.dart';
import '../../data/models/auth_models.dart';
import '../providers/auth_provider.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _documentController = TextEditingController();
  final _pinController = TextEditingController();
  String _documentType = 'CC';
  bool _isLoading = false;

  @override
  void dispose() {
    _documentController.dispose();
    _pinController.dispose();
    super.dispose();
  }

  Future<void> _loginWithPin() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _isLoading = true);

    await ref.read(authStateProvider.notifier).loginWithPin(
          LoginRequest(
            documentType: _documentType,
            documentNumber: _documentController.text.trim(),
            pin: _pinController.text,
          ),
        );

    if (!mounted) return;
    setState(() => _isLoading = false);

    final state = ref.read(authStateProvider);
    if (state.hasError) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(state.error.toString())),
      );
      return;
    }
    if (state.hasValue && state.value != null) {
      context.go('/home');
    }
  }

  Future<void> _loginWithBiometric() async {
    setState(() => _isLoading = true);

    await ref.read(authStateProvider.notifier).loginWithBiometric(
          LoginRequest(
            documentType: _documentType,
            documentNumber: _documentController.text.trim(),
            biometricPayload: 'device-biometric-token',
          ),
        );

    if (!mounted) return;
    setState(() => _isLoading = false);

    final state = ref.read(authStateProvider);
    if (state.hasValue && state.value != null) {
      context.go('/home');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Iniciar sesión')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Bienvenido',
                  style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 8),
                const Text('Ingresa con tu documento y PIN de 4 dígitos'),
                const SizedBox(height: 24),
                DropdownButtonFormField<String>(
                  value: _documentType,
                  decoration: const InputDecoration(labelText: 'Tipo de documento'),
                  items: const [
                    DropdownMenuItem(value: 'CC', child: Text('Cédula de ciudadanía')),
                    DropdownMenuItem(value: 'CE', child: Text('Cédula de extranjería')),
                  ],
                  onChanged: (value) => setState(() => _documentType = value ?? 'CC'),
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _documentController,
                  decoration: const InputDecoration(labelText: 'Número de documento'),
                  keyboardType: TextInputType.number,
                  validator: (value) =>
                      value == null || value.isEmpty ? 'Campo requerido' : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _pinController,
                  decoration: const InputDecoration(labelText: 'PIN (4 dígitos)'),
                  keyboardType: TextInputType.number,
                  obscureText: true,
                  maxLength: 4,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  validator: (value) {
                    if (value == null || value.length != 4) {
                      return 'El PIN debe tener 4 dígitos';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 24),
                AccessibleButton(
                  label: 'Ingresar con PIN',
                  isLoading: _isLoading,
                  onPressed: _loginWithPin,
                ),
                const SizedBox(height: 12),
                AccessibleButton(
                  label: 'Ingresar con biometría',
                  semanticLabel: 'Ingresar usando huella o reconocimiento facial',
                  isLoading: _isLoading,
                  onPressed: _loginWithBiometric,
                ),
                const SizedBox(height: 24),
                TextButton(
                  onPressed: () => context.push('/register'),
                  child: const Text('¿No tienes cuenta? Regístrate'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
