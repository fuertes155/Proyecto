import 'dart:async';
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
  bool _isResending = false;
  String? _error;

  // Temporizador de reenvío
  int _resendCooldown = 0;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    // Al entrar, iniciar el contador (el correo ya fue enviado al registrarse)
    _startCooldown();
  }

  @override
  void dispose() {
    _timer?.cancel();
    _otpController.dispose();
    super.dispose();
  }

  void _startCooldown() {
    setState(() => _resendCooldown = 60);
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), (t) {
      if (_resendCooldown <= 1) {
        t.cancel();
        setState(() => _resendCooldown = 0);
      } else {
        setState(() => _resendCooldown--);
      }
    });
  }

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
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _resendOtp() async {
    if (_resendCooldown > 0 || _isResending) return;

    setState(() {
      _isResending = true;
      _error = null;
    });

    try {
      final dio = ref.read(apiClientProvider);
      await dio.post('/v1/auth/resend-email-otp', data: {
        'documentType': widget.documentType,
        'documentNumber': widget.documentNumber,
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Código reenviado. Revisa tu correo.')),
        );
        _otpController.clear();
        _startCooldown();
      }
    } on DioException catch (e) {
      final responseData = e.response?.data;
      final errorMsg = responseData is Map ? responseData['message']?.toString() : null;
      if (mounted) {
        setState(() => _error = errorMsg ?? 'No se pudo reenviar el código. Intenta de nuevo.');
      }
    } finally {
      if (mounted) setState(() => _isResending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).colorScheme.primary;
    final canResend = _resendCooldown == 0 && !_isResending;

    return Scaffold(
      appBar: AppBar(title: const Text('Verificar Correo')),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.mark_email_read_outlined, size: 72, color: primaryColor),
            const SizedBox(height: 24),
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
              style: const TextStyle(fontSize: 28, letterSpacing: 12, fontWeight: FontWeight.bold),
              decoration: InputDecoration(
                hintText: '------',
                counterText: '',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              ),
              onChanged: (_) => setState(() => _error = null),
            ),
            if (_error != null) ...[
              const SizedBox(height: 12),
              Text(_error!, style: const TextStyle(color: Colors.red), textAlign: TextAlign.center),
            ],
            const SizedBox(height: 32),
            SizedBox(
              width: double.infinity,
              height: 52,
              child: FilledButton(
                onPressed: _isLoading ? null : _verifyEmail,
                child: _isLoading 
                    ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                    : const Text('Verificar', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              ),
            ),
            const SizedBox(height: 20),

            // Botón reenviar con contador
            TextButton.icon(
              onPressed: canResend ? _resendOtp : null,
              icon: _isResending
                  ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Icon(Icons.refresh_rounded, size: 18),
              label: Text(
                _resendCooldown > 0
                    ? 'Reenviar código en ${_resendCooldown}s'
                    : 'Reenviar código',
                style: TextStyle(
                  color: canResend ? primaryColor : Colors.grey,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
