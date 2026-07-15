import 'package:flutter/material.dart';

class OtpMandateDialog extends StatefulWidget {
  final VoidCallback onValidSignature;
  
  const OtpMandateDialog({super.key, required this.onValidSignature});

  @override
  State<OtpMandateDialog> createState() => _OtpMandateDialogState();
}

class _OtpMandateDialogState extends State<OtpMandateDialog> {
  final _otpController = TextEditingController();
  bool _isVerifying = false;

  void _verifyOtp() async {
    if (_otpController.text.length < 6) return;
    
    setState(() => _isVerifying = true);
    
    // Simular llamada al backend para validar OTP
    await Future.delayed(const Duration(seconds: 2));
    
    if (!mounted) return;
    
    // Simular firma legal completada
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Contrato firmado exitosamente. Hash SHA-256 guardado.')),
    );
    
    widget.onValidSignature();
  }

  @override
  void dispose() {
    _otpController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: Container(
        padding: const EdgeInsets.all(24),
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Icon(Icons.security, size: 48, color: Color(0xFF00C853)),
            const SizedBox(height: 16),
            const Text(
              'Firma de Mandato Electrónico',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Text(
              'Hemos enviado un código SMS para firmar legalmente tu contrato de inversión.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.black54),
            ),
            const SizedBox(height: 24),
            TextField(
              controller: _otpController,
              keyboardType: TextInputType.number,
              maxLength: 6,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 24, letterSpacing: 8, fontWeight: FontWeight.bold),
              decoration: InputDecoration(
                hintText: '000000',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              ),
              onChanged: (val) {
                if (val.length == 6) {
                  _verifyOtp();
                }
              },
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _isVerifying ? null : _verifyOtp,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF2C3545),
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              child: _isVerifying
                  ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                  : const Text('Firmar Contrato', style: TextStyle(fontWeight: FontWeight.bold)),
            ),
            const SizedBox(height: 16),
            TextButton.icon(
              onPressed: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Descargando PDF del contrato...')),
                );
              },
              icon: const Icon(Icons.picture_as_pdf),
              label: const Text('Ver Borrador del Contrato'),
            )
          ],
        ),
      ),
    );
  }
}
