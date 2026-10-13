import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:dio/dio.dart';

import '../../../../core/widgets/accessible_button.dart';
import '../../data/models/auth_models.dart';
import '../../data/repositories/auth_repository_impl.dart';

class RegisterPage extends ConsumerStatefulWidget {
  const RegisterPage({super.key});

  @override
  ConsumerState<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends ConsumerState<RegisterPage> {
  final _formKey = GlobalKey<FormState>();
  final _documentController = TextEditingController();
  final _fullNameController = TextEditingController();
  final _pinController = TextEditingController();
  String _documentType = 'CC';
  bool _isLoading = false;
  bool _termsAccepted = false;

  @override
  void dispose() {
    _documentController.dispose();
    _fullNameController.dispose();
    _pinController.dispose();
    super.dispose();
  }

  Future<void> _register() async {
    if (!_formKey.currentState!.validate()) return;
    if (!_termsAccepted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Debes aceptar los Términos y Condiciones')),
      );
      return;
    }
    setState(() => _isLoading = true);

    try {
      final repository = ref.read(authRepositoryProvider);
      await repository.register(RegisterRequest(
        documentType: _documentType,
        documentNumber: _documentController.text.trim(),
        fullName: _fullNameController.text.trim(),
        pin: _pinController.text,
        termsAccepted: _termsAccepted,
      ));

      if (!mounted) return;
      // Sin correo que verificar la cuenta queda activa: se entra de una vez con cédula y PIN.
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Cuenta creada. Ingresa con tu cédula y tu PIN.')),
      );
      context.go('/login');
    } on DioException catch (e) {
      if (!mounted) return;
      final responseData = e.response?.data;
      final errorMsg = responseData is Map ? responseData['message']?.toString() : null;
      final msg = errorMsg ?? e.message ?? e.toString();

      String friendlyMsg = msg;
      if (msg.toLowerCase().contains('documento') || msg.toLowerCase().contains('user_already_exists')) {
        friendlyMsg = 'Este documento ya está registrado.';
      }

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(friendlyMsg),
          backgroundColor: Colors.redAccent,
        ),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error inesperado: $error'),
          backgroundColor: Colors.redAccent,
        ),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Crear cuenta')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              children: [
                TextFormField(
                  controller: _documentController,
                  decoration: InputDecoration(
                    labelText: _documentType == 'CE' ? 'Número de cédula de extranjería' : 'Número de cédula',
                  ),
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  validator: (v) => v == null || v.isEmpty ? 'Requerido' : null,
                ),
                // El tipo de documento ya no es un campo: se asume cédula de ciudadanía y
                // quien tenga cédula de extranjería lo indica con este enlace.
                Align(
                  alignment: Alignment.centerLeft,
                  child: TextButton(
                    onPressed: () => setState(() => _documentType = _documentType == 'CC' ? 'CE' : 'CC'),
                    child: Text(_documentType == 'CC'
                        ? '¿Tu cédula es de extranjería?'
                        : 'Usar cédula de ciudadanía'),
                  ),
                ),
                TextFormField(
                  controller: _fullNameController,
                  decoration: const InputDecoration(labelText: 'Nombre completo'),
                  keyboardType: TextInputType.name,
                  textCapitalization: TextCapitalization.words,
                  inputFormatters: [LengthLimitingTextInputFormatter(100)],
                  validator: (v) => v == null || v.trim().split(RegExp(r'\s+')).length < 2
                      ? 'Escribe tu nombre y apellido'
                      : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _pinController,
                  decoration: const InputDecoration(labelText: 'PIN de 4 dígitos'),
                  obscureText: true,
                  maxLength: 4,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  validator: (v) => v == null || v.length != 4 ? 'PIN inválido' : null,
                ),
                const SizedBox(height: 16),
                CheckboxListTile(
                  title: GestureDetector(
                    onTap: () {
                      showDialog(
                        context: context,
                        builder: (ctx) => AlertDialog(
                          title: const Text('Políticas y Términos'),
                          content: const Text('Aquí va el texto legal de las políticas de privacidad y términos y condiciones.'),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.of(ctx).pop(),
                              child: const Text('Cerrar'),
                            )
                          ],
                        ),
                      );
                    },
                    child: const Text(
                      'Acepto las políticas de privacidad y términos y condiciones',
                      style: TextStyle(
                        decoration: TextDecoration.underline,
                        color: Colors.blue,
                      ),
                    ),
                  ),
                  value: _termsAccepted,
                  onChanged: (val) {
                    setState(() => _termsAccepted = val ?? false);
                  },
                  controlAffinity: ListTileControlAffinity.leading,
                  contentPadding: EdgeInsets.zero,
                ),
                const SizedBox(height: 24),
                AccessibleButton(
                  label: 'Registrarme',
                  isLoading: _isLoading,
                  onPressed: _register,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
