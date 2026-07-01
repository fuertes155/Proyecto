import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/admin_repository.dart';
import '../../data/models/admin_models.dart';

class EmergencyLockPage extends ConsumerStatefulWidget {
  const EmergencyLockPage({super.key});

  @override
  ConsumerState<EmergencyLockPage> createState() => _EmergencyLockPageState();
}

class _EmergencyLockPageState extends ConsumerState<EmergencyLockPage> {
  final _formKey = GlobalKey<FormState>();
  final _targetIdCtrl = TextEditingController();
  final _reasonCtrl = TextEditingController();
  String _scope = 'USER';
  bool _isLoading = false;

  final _scopes = [
    ('USER', 'Suspender cuenta de usuario', Icons.person_off_rounded),
    ('ACCESS_TOKEN', 'Revocar todos los tokens', Icons.token_rounded),
  ];

  @override
  void dispose() {
    _targetIdCtrl.dispose();
    _reasonCtrl.dispose();
    super.dispose();
  }

  Future<void> _execute() async {
    if (!_formKey.currentState!.validate()) return;

    // Confirmación obligatoria
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1A0A0A),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Row(children: [
          Icon(Icons.warning_amber_rounded, color: Color(0xFF53A835), size: 28),
          SizedBox(width: 10),
          Text('¿Confirmar bloqueo?',
              style: TextStyle(color: Colors.white, fontSize: 18)),
        ]),
        content: Text(
          'Esta acción suspenderá el acceso del usuario indicado de forma inmediata.\n\nMotivo: ${_reasonCtrl.text}\nAlcance: $_scope',
          style: TextStyle(color: Colors.white.withOpacity(0.8)),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar', style: TextStyle(color: Colors.white60)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF53A835)),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('SÍ, BLOQUEAR', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    setState(() => _isLoading = true);
    try {
      await ref.read(adminRepositoryProvider).emergencyLock(EmergencyLockRequest(
            targetId: _targetIdCtrl.text.trim(),
            scope: _scope,
            reason: _reasonCtrl.text.trim(),
          ));
      if (!mounted) return;
      _showSuccess('✓ Bloqueo de emergencia ejecutado con éxito');
      _targetIdCtrl.clear();
      _reasonCtrl.clear();
    } catch (e) {
      _showError('Error: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showSuccess(String msg) => ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg), backgroundColor: const Color(0xFF00A86B),
          behavior: SnackBarBehavior.floating));

  void _showError(String msg) => ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg), backgroundColor: const Color(0xFFCF3232),
          behavior: SnackBarBehavior.floating));

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0D0D0D),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        foregroundColor: Colors.white,
        title: const Row(children: [
          Icon(Icons.emergency_rounded, color: Color(0xFF53A835), size: 24),
          SizedBox(width: 10),
          Text('Bloqueo de Emergencia',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        ]),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Advertencia
              _WarningBanner(
                  'Esta acción es inmediata, irreversible sin autorización, y queda registrada en el log de auditoría.'),
              const SizedBox(height: 24),
              // ID objetivo
              _buildLabel('ID del objetivo (UUID del usuario)'),
              const SizedBox(height: 8),
              TextFormField(
                controller: _targetIdCtrl,
                style: const TextStyle(color: Colors.white),
                decoration: _inputDeco('Ej: 123e4567-e89b-12d3-a456-426614174000',
                    Icons.fingerprint_rounded),
                validator: (v) => v == null || v.isEmpty ? 'Campo requerido' : null,
              ),
              const SizedBox(height: 20),
              // Alcance
              _buildLabel('Alcance del bloqueo'),
              const SizedBox(height: 10),
              ..._scopes.map((s) => _ScopeOption(
                    label: s.$1,
                    subtitle: s.$2,
                    icon: s.$3,
                    selected: _scope == s.$1,
                    onTap: () => setState(() => _scope = s.$1),
                  )),
              const SizedBox(height: 20),
              // Motivo
              _buildLabel('Motivo del bloqueo *'),
              const SizedBox(height: 8),
              TextFormField(
                controller: _reasonCtrl,
                maxLines: 3,
                style: const TextStyle(color: Colors.white),
                decoration: _inputDeco('Describe el motivo detalladamente...',
                    Icons.description_outlined),
                validator: (v) =>
                    v == null || v.length < 10 ? 'Mínimo 10 caracteres' : null,
              ),
              const SizedBox(height: 32),
              // Botón
              SizedBox(
                height: 56,
                child: ElevatedButton.icon(
                  onPressed: _isLoading ? null : _execute,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF53A835),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16)),
                    elevation: 8,
                    shadowColor: const Color(0xFF53A835).withOpacity(0.5),
                  ),
                  icon: _isLoading
                      ? const SizedBox(width: 20, height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                      : const Icon(Icons.block_rounded),
                  label: const Text('EJECUTAR BLOQUEO',
                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold,
                          letterSpacing: 1)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildLabel(String text) => Text(text,
      style: const TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600));

  InputDecoration _inputDeco(String hint, IconData icon) => InputDecoration(
        hintText: hint,
        hintStyle: TextStyle(color: Colors.white.withOpacity(0.3)),
        prefixIcon: Icon(icon, color: Colors.white38),
        filled: true,
        fillColor: Colors.white.withOpacity(0.05),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide.none),
        enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: BorderSide(color: Colors.white.withOpacity(0.1))),
        focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: const BorderSide(color: Color(0xFF53A835), width: 1.5)),
      );
}

class _WarningBanner extends StatelessWidget {
  const _WarningBanner(this.text);
  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFF53A835).withOpacity(0.15),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFF53A835).withOpacity(0.4)),
      ),
      child: Row(
        children: [
          const Icon(Icons.warning_rounded, color: Color(0xFFFF6B00), size: 22),
          const SizedBox(width: 12),
          Expanded(
            child: Text(text,
                style: TextStyle(color: Colors.white.withOpacity(0.85), fontSize: 12, height: 1.4)),
          ),
        ],
      ),
    );
  }
}

class _ScopeOption extends StatelessWidget {
  const _ScopeOption({
    required this.label,
    required this.subtitle,
    required this.icon,
    required this.selected,
    required this.onTap,
  });
  final String label;
  final String subtitle;
  final IconData icon;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: selected
              ? const Color(0xFF53A835).withOpacity(0.2)
              : Colors.white.withOpacity(0.04),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
              color: selected
                  ? const Color(0xFF53A835).withOpacity(0.7)
                  : Colors.white.withOpacity(0.1)),
        ),
        child: Row(
          children: [
            Icon(icon,
                color: selected ? const Color(0xFF53A835) : Colors.white38, size: 24),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(label,
                      style: TextStyle(
                          color: selected ? Colors.white : Colors.white70,
                          fontWeight: FontWeight.w600,
                          fontSize: 14)),
                  Text(subtitle,
                      style: TextStyle(color: Colors.white.withOpacity(0.4), fontSize: 11)),
                ],
              ),
            ),
            if (selected)
              const Icon(Icons.check_circle_rounded, color: Color(0xFF53A835), size: 22),
          ],
        ),
      ),
    );
  }
}
