import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:dio/dio.dart';
import '../../../../core/network/api_client_provider.dart';

class EmailVerificationPage extends ConsumerStatefulWidget {
  final String documentType;
  final String documentNumber;
  
  const EmailVerificationPage({
    super.key, 
    required this.documentType, 
    required this.documentNumber
  });

  @override
  ConsumerState<EmailVerificationPage> createState() => _EmailVerificationPageState();
}

class _EmailVerificationPageState extends ConsumerState<EmailVerificationPage> {
  final _otpController = TextEditingController();
  bool _isLoading = false;
  String? _error;

  Future<void> _verifyEmail() async {
    if (_otpController.text.length != 6) return;

    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final dio = ref.read(apiClientProvider);
      await dio.post('/v1/auth/verify-email', data: {
        'documentType': widget.documentType,
        'documentNumber': widget.documentNumber,
        'otpCode': _otpController.text,
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Correo verificado exitosamente. Ya puedes iniciar sesión.')),
        );
        context.go('/login');
      }
    } on DioException catch (e) {
      final responseData = e.response?.data;
      final errorMsg = responseData is Map ? responseData['message']?.toString() : null;
      setState(() {
        _error = errorMsg ?? 'Código inválido o expirado';
      });
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Verificar Correo')),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text(
              'Hemos enviado un código a tu correo electrónico. Ingrésalo a continuación para verificar tu cuenta.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 16),
            ),
            const SizedBox(height: 32),
            TextField(
              controller: _otpController,
              keyboardType: TextInputType.number,
              maxLength: 6,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 24, letterSpacing: 8),
              decoration: const InputDecoration(
                hintText: '000000',
              ),
            ),
            if (_error != null) ...[
              const SizedBox(height: 16),
              Text(_error!, style: const TextStyle(color: Colors.red)),
            ],
            const SizedBox(height: 32),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: _isLoading ? null : _verifyEmail,
                child: _isLoading 
                    ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                    : const Text('Verificar'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
