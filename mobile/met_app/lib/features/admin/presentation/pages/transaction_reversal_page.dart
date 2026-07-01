import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/admin_provider.dart';
import '../../data/repositories/admin_repository.dart';

class TransactionReversalPage extends ConsumerStatefulWidget {
  const TransactionReversalPage({super.key});

  @override
  ConsumerState<TransactionReversalPage> createState() =>
      _TransactionReversalPageState();
}

class _TransactionReversalPageState
    extends ConsumerState<TransactionReversalPage> {
  final _txIdCtrl = TextEditingController();
  final _reasonCtrl = TextEditingController();
  final _codeCtrl = TextEditingController();
  bool _isLoading = false;

  Future<void> _submitReversal() async {
    if (_txIdCtrl.text.isEmpty ||
        _reasonCtrl.text.isEmpty ||
        _codeCtrl.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
        content: Text('Por favor, completa todos los campos'),
        backgroundColor: Color(0xFFCF3232),
      ));
      return;
    }

    setState(() => _isLoading = true);
    try {
      await ref.read(adminRepositoryProvider).reverseTransaction(
            _txIdCtrl.text,
            _reasonCtrl.text,
            _codeCtrl.text,
          );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          content: Text('Transacción reversada exitosamente'),
          backgroundColor: Color(0xFF53A835),
        ));
        _txIdCtrl.clear();
        _reasonCtrl.clear();
        _codeCtrl.clear();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text('Error al reversar: $e'),
          backgroundColor: const Color(0xFFCF3232),
        ));
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    const primaryColor = Color(0xFF53A835);

    return Scaffold(
      backgroundColor: const Color(0xFF0D0D0D),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        foregroundColor: Colors.white,
        title: const Row(children: [
          Icon(Icons.undo_rounded, color: primaryColor, size: 24),
          SizedBox(width: 10),
          Text('Reversión de Transacción',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        ]),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: primaryColor.withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: primaryColor.withOpacity(0.3)),
              ),
              child: const Row(
                children: [
                  Icon(Icons.warning_amber_rounded, color: primaryColor),
                  SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      'Esta acción es irreversible y quedará registrada en la bitácora de auditoría. '
                      'Requiere código de confirmación del administrador superior.',
                      style: TextStyle(color: Colors.white70, fontSize: 13),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 30),
            _InputField(
              label: 'ID de la Transacción',
              controller: _txIdCtrl,
              icon: Icons.receipt_long_rounded,
            ),
            const SizedBox(height: 16),
            _InputField(
              label: 'Motivo de la reversión',
              controller: _reasonCtrl,
              icon: Icons.notes_rounded,
              maxLines: 3,
            ),
            const SizedBox(height: 16),
            _InputField(
              label: 'Código de Confirmación',
              controller: _codeCtrl,
              icon: Icons.password_rounded,
              isPassword: true,
            ),
            const SizedBox(height: 40),
            SizedBox(
              width: double.infinity,
              height: 56,
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: primaryColor,
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16)),
                  elevation: 0,
                ),
                onPressed: _isLoading ? null : _submitReversal,
                child: _isLoading
                    ? const CircularProgressIndicator(color: Colors.white)
                    : const Text(
                        'EJECUTAR REVERSIÓN',
                        style: TextStyle(
                            color: Colors.white,
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 1),
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _InputField extends StatelessWidget {
  const _InputField({
    required this.label,
    required this.controller,
    required this.icon,
    this.maxLines = 1,
    this.isPassword = false,
  });

  final String label;
  final TextEditingController controller;
  final IconData icon;
  final int maxLines;
  final bool isPassword;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label,
            style: const TextStyle(
                color: Colors.white70,
                fontSize: 14,
                fontWeight: FontWeight.w500)),
        const SizedBox(height: 8),
        TextField(
          controller: controller,
          maxLines: maxLines,
          obscureText: isPassword,
          style: const TextStyle(color: Colors.white),
          decoration: InputDecoration(
            prefixIcon: Icon(icon, color: Colors.white38),
            filled: true,
            fillColor: Colors.white.withOpacity(0.05),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: BorderSide.none,
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: const BorderSide(color: Colors.white12),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: const BorderSide(color: Color(0xFF53A835)),
            ),
          ),
        ),
      ],
    );
  }
}
