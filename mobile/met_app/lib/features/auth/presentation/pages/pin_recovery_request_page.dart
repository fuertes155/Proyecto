import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../providers/auth_provider.dart';
import '../../../../core/utils/error_mapper.dart';

class PinRecoveryRequestPage extends ConsumerStatefulWidget {
  const PinRecoveryRequestPage({super.key});

  @override
  ConsumerState<PinRecoveryRequestPage> createState() => _PinRecoveryRequestPageState();
}

class _PinRecoveryRequestPageState extends ConsumerState<PinRecoveryRequestPage> {
  final _formKey = GlobalKey<FormState>();
  String _docType = 'CC';
  final _docNumberController = TextEditingController();
  bool _isLoading = false;

  @override
  void dispose() {
    _docNumberController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);
    try {
      await ref.read(authStateProvider.notifier).requestPinRecovery(
        documentType: _docType,
        documentNumber: _docNumberController.text,
      );
      if (mounted) {
        // Navegar al siguiente paso
        context.push(
          '/auth/recover-pin/verify',
          extra: {
            'documentType': _docType,
            'documentNumber': _docNumberController.text,
          },
        );
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
      appBar: AppBar(title: const Text('Recuperar PIN')),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Ingresa tu documento para verificar tu identidad y enviarte un código.',
                style: TextStyle(fontSize: 16),
              ),
              const SizedBox(height: 24),
              DropdownButtonFormField<String>(
                value: _docType,
                decoration: const InputDecoration(labelText: 'Tipo de Documento'),
                items: ['CC', 'CE', 'PASSPORT', 'NIT'].map((type) {
                  return DropdownMenuItem(value: type, child: Text(type));
                }).toList(),
                onChanged: (val) => setState(() => _docType = val!),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _docNumberController,
                decoration: const InputDecoration(labelText: 'Número de Documento'),
                keyboardType: TextInputType.number,
                validator: (v) => v!.isEmpty ? 'Requerido' : null,
              ),
              const SizedBox(height: 32),
              ElevatedButton(
                onPressed: _isLoading ? null : _submit,
                child: _isLoading
                    ? const CircularProgressIndicator(color: Colors.white)
                    : const Text('Enviar código', style: TextStyle(color: Colors.white)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
