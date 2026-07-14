import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../providers/auth_provider.dart';
import '../../../../core/utils/error_mapper.dart';

class PinRecoveryVerifyPage extends ConsumerStatefulWidget {
  final String documentType;
  final String documentNumber;

  const PinRecoveryVerifyPage({
    super.key,
    required this.documentType,
    required this.documentNumber,
  });

  @override
  ConsumerState<PinRecoveryVerifyPage> createState() => _PinRecoveryVerifyPageState();
}

class _PinRecoveryVerifyPageState extends ConsumerState<PinRecoveryVerifyPage> {
  final _formKey = GlobalKey<FormState>();
  final _otpController = TextEditingController();
  final _newPinController = TextEditingController();
  final _confirmPinController = TextEditingController();
  bool _isLoading = false;

  @override
  void dispose() {
    _otpController.dispose();
    _newPinController.dispose();
    _confirmPinController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    if (_newPinController.text != _confirmPinController.text) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Los PINs no coinciden'), backgroundColor: Colors.red),
      );
      return;
    }

    setState(() => _isLoading = true);
    try {
      await ref.read(authStateProvider.notifier).resetPinWithOtp(
        documentType: widget.documentType,
        documentNumber: widget.documentNumber,
        otpCode: _otpController.text,
        newPin: _newPinController.text,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('PIN actualizado correctamente')),
        );
        // Volver al login
        context.go('/login');
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(ErrorMapper.toUserMessage(e)),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Crear Nuevo PIN')),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'Ingresa el código de 6 dígitos que enviamos a tu número/correo asociado al documento ${widget.documentNumber}.',
                style: const TextStyle(fontSize: 16),
              ),
              const SizedBox(height: 24),
              TextFormField(
                controller: _otpController,
                decoration: const InputDecoration(labelText: 'Código de verificación'),
                keyboardType: TextInputType.number,
                maxLength: 6,
                validator: (v) => v!.isEmpty ? 'Requerido' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _newPinController,
                decoration: const InputDecoration(labelText: 'Nuevo PIN (4 a 6 dígitos)'),
                keyboardType: TextInputType.number,
                obscureText: true,
                validator: (v) {
                  if (v!.isEmpty) return 'Requerido';
                  if (v.length < 4 || v.length > 6) return 'Debe tener entre 4 y 6 dígitos';
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
                onPressed: _isLoading ? null : _submit,
                child: _isLoading
                    ? const CircularProgressIndicator(color: Colors.white)
                    : const Text('Confirmar PIN', style: TextStyle(color: Colors.white)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
