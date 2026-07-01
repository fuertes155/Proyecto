import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/admin_provider.dart';
import '../../data/repositories/admin_repository.dart';

class ResetCredentialsPage extends ConsumerStatefulWidget {
  const ResetCredentialsPage({super.key});

  @override
  ConsumerState<ResetCredentialsPage> createState() =>
      _ResetCredentialsPageState();
}

class _ResetCredentialsPageState extends ConsumerState<ResetCredentialsPage> {
  final _userIdCtrl = TextEditingController();
  final _reasonCtrl = TextEditingController();
  bool _isLoading = false;

  Future<void> _submitReset() async {
    if (_userIdCtrl.text.isEmpty || _reasonCtrl.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
        content: Text('Por favor, completa todos los campos'),
        backgroundColor: Color(0xFFCF3232),
      ));
      return;
    }

    setState(() => _isLoading = true);
    try {
      await ref.read(adminRepositoryProvider).resetUserCredentials(
            _userIdCtrl.text,
            _reasonCtrl.text,
          );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          content: Text('Credenciales reseteadas exitosamente'),
          backgroundColor: Color(0xFF53A835),
        ));
        _userIdCtrl.clear();
        _reasonCtrl.clear();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text('Error al resetear credenciales: $e'),
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
          Icon(Icons.lock_reset_rounded, color: primaryColor, size: 24),
          SizedBox(width: 10),
          Expanded(child: Text('Reseteo de Credenciales',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold))),
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
                  Icon(Icons.info_outline_rounded, color: primaryColor),
                  SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      'Esta acción invalidará las credenciales actuales del usuario '
                      'y le enviará un correo con un enlace seguro para establecer '
                      'una nueva contraseña y PIN biométrico.',
                      style: TextStyle(color: Colors.white70, fontSize: 13),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 30),
            _InputField(
              label: 'ID del Usuario (Email o UUID)',
              controller: _userIdCtrl,
              icon: Icons.person_search_rounded,
            ),
            const SizedBox(height: 16),
            _InputField(
              label: 'Motivo del reseteo (Ej. Solicitud por pérdida)',
              controller: _reasonCtrl,
              icon: Icons.notes_rounded,
              maxLines: 3,
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
                onPressed: _isLoading ? null : _submitReset,
                child: _isLoading
                    ? const CircularProgressIndicator(color: Colors.white)
                    : const Text(
                        'FORZAR RESETEO',
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
  });

  final String label;
  final TextEditingController controller;
  final IconData icon;
  final int maxLines;

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
