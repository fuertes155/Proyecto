import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/models/auth_models.dart';
import '../../data/repositories/auth_repository_impl.dart';
import '../../data/services/biometric_login_service.dart';
import '../providers/auth_provider.dart';

class SecuritySettingsPage extends ConsumerStatefulWidget {
  const SecuritySettingsPage({super.key});

  @override
  ConsumerState<SecuritySettingsPage> createState() => _SecuritySettingsPageState();
}

class _SecuritySettingsPageState extends ConsumerState<SecuritySettingsPage> {
  final _formKey = GlobalKey<FormState>();
  final _currentPinController = TextEditingController();
  final _newPinController = TextEditingController();
  final _confirmPinController = TextEditingController();
  bool _isLoading = false;

  // Ingreso con huella
  bool _biometricAvailable = false;
  bool _biometricEnabled = false;
  bool _biometricEnabledElsewhere = false;
  bool _biometricBusy = false;
  UserResponse? _profile;

  @override
  void initState() {
    super.initState();
    _loadBiometricState();
  }

  @override
  void dispose() {
    _currentPinController.dispose();
    _newPinController.dispose();
    _confirmPinController.dispose();
    super.dispose();
  }

  Future<void> _loadBiometricState() async {
    final biometric = ref.read(biometricLoginServiceProvider);
    if (!await biometric.isAvailable()) return;

    final credential = await biometric.readCredential();
    UserResponse? profile;
    try {
      profile = await ref.read(authRepositoryProvider).getProfile();
    } catch (_) {
      profile = ref.read(authStateProvider).valueOrNull;
    }
    if (profile == null) return;

    final enabledOnServer = profile.biometricLoginEnabled;
    final credentialIsMine = credential != null &&
        credential.documentType == profile.documentType &&
        credential.documentNumber == profile.documentNumber;
    // La llave de este celular ya no sirve (se activó en otro celular, la desactivó un
    // administrador o es de otra persona): se borra para que el login no la ofrezca.
    if (credential != null && (!enabledOnServer || !credentialIsMine)) {
      await biometric.forget();
    }

    if (!mounted) return;
    setState(() {
      _profile = profile;
      _biometricAvailable = true;
      _biometricEnabled = enabledOnServer && credentialIsMine;
      _biometricEnabledElsewhere = enabledOnServer && !credentialIsMine;
    });
  }

  Future<void> _toggleBiometric(bool enable) async {
    final biometric = ref.read(biometricLoginServiceProvider);
    final profile = _profile;
    if (profile == null) return;

    String? pin;
    if (enable) {
      pin = await _askCurrentPin();
      if (pin == null) return;
    }

    setState(() => _biometricBusy = true);
    try {
      if (enable) {
        final activated = await biometric.enable(
          pin: pin!,
          documentType: profile.documentType,
          documentNumber: profile.documentNumber,
        );
        if (!activated || !mounted) return;
        setState(() {
          _biometricEnabled = true;
          _biometricEnabledElsewhere = false;
        });
        _showMessage('Listo. La próxima vez puedes entrar con tu huella.');
      } else {
        await biometric.disable();
        if (!mounted) return;
        setState(() => _biometricEnabled = false);
        _showMessage('Desactivaste el ingreso con huella.');
      }
    } on DioException catch (e) {
      final data = e.response?.data;
      final message = data is Map ? data['message'] as String? : null;
      _showMessage(message ?? 'No pudimos actualizar el ingreso con huella. Intenta de nuevo.', error: true);
    } catch (_) {
      _showMessage('No pudimos actualizar el ingreso con huella. Intenta de nuevo.', error: true);
    } finally {
      if (mounted) setState(() => _biometricBusy = false);
    }
  }

  /// Confirmar el PIN asegura que quien activa la huella es el titular de la cuenta.
  Future<String?> _askCurrentPin() {
    final controller = TextEditingController();
    return showDialog<String>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Confirma tu PIN'),
        content: TextField(
          controller: controller,
          autofocus: true,
          obscureText: true,
          keyboardType: TextInputType.number,
          maxLength: 6,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          decoration: const InputDecoration(labelText: 'PIN actual', counterText: ''),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: const Text('Cancelar'),
          ),
          TextButton(
            onPressed: () {
              if (controller.text.length < 4) return;
              Navigator.of(dialogContext).pop(controller.text);
            },
            child: const Text('Continuar'),
          ),
        ],
      ),
    );
  }

  void _showMessage(String message, {bool error = false}) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: error ? Colors.red : null),
    );
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;

    if (_newPinController.text != _confirmPinController.text) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('El nuevo PIN no coincide'), backgroundColor: Colors.red),
      );
      return;
    }

    setState(() => _isLoading = true);
    try {
      await ref.read(authStateProvider.notifier).updatePin(
        currentPin: _currentPinController.text,
        newPin: _newPinController.text,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('PIN actualizado correctamente')),
        );
        Navigator.pop(context);
      }
    } catch (e) {
      // El mensaje del backend ("El PIN actual es incorrecto"), no el texto técnico de DioException.
      final data = e is DioException ? e.response?.data : null;
      final message = data is Map ? data['message'] as String? : null;
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(message ?? 'No pudimos actualizar el PIN. Intenta de nuevo.'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Seguridad y Contraseña')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (_biometricAvailable) ...[
                const Text(
                  'Ingreso con huella',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 8),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  secondary: const Icon(Icons.fingerprint, size: 32),
                  title: const Text('Entrar con mi huella'),
                  subtitle: Text(
                    _biometricEnabledElsewhere
                        ? 'Está activa en otro celular. Si la activas aquí, se desactiva allá.'
                        : 'Usa la huella o el rostro registrados en este celular en lugar del PIN.',
                  ),
                  value: _biometricEnabled,
                  onChanged: _biometricBusy ? null : _toggleBiometric,
                ),
                const Divider(height: 40),
              ],
              const Text(
                'Cambiar PIN de seguridad',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 24),
              TextFormField(
                controller: _currentPinController,
                decoration: const InputDecoration(labelText: 'PIN actual'),
                keyboardType: TextInputType.number,
                obscureText: true,
                validator: (v) => v!.isEmpty ? 'Requerido' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _newPinController,
                decoration: const InputDecoration(labelText: 'Nuevo PIN (4 dígitos)'),
                keyboardType: TextInputType.number,
                obscureText: true,
                // Exactamente 4 dígitos, como en el registro y la recuperación: el teclado del
                // ingreso solo admite 4, así que con 5 o 6 el socio ya no podría entrar.
                validator: (v) {
                  if (v!.isEmpty) return 'Requerido';
                  if (!RegExp(r'^\d{4}$').hasMatch(v)) return 'El PIN debe tener 4 dígitos';
                  return null;
                },
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _confirmPinController,
                decoration: const InputDecoration(labelText: 'Confirmar nuevo PIN'),
                keyboardType: TextInputType.number,
                obscureText: true,
                validator: (v) => v!.isEmpty ? 'Requerido' : null,
              ),
              const SizedBox(height: 32),
              ElevatedButton(
                onPressed: _isLoading ? null : _save,
                child: _isLoading
                    ? const CircularProgressIndicator(color: Colors.white)
                    : const Text('Actualizar PIN', style: TextStyle(color: Colors.white)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
