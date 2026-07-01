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
        backgroundColor: Theme.of(context).colorScheme.surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Row(children: [
          Icon(Icons.warning_amber_rounded, color: Theme.of(context).colorScheme.primary, size: 28),
          const SizedBox(width: 10),
          Text('¿Confirmar bloqueo?',
              style: TextStyle(color: Theme.of(context).colorScheme.onSurface, fontSize: 18)),
        ]),
        content: Text(
          'Esta acción suspenderá el acceso del usuario indicado de forma inmediata.\n\nMotivo: ${_reasonCtrl.text}\nAlcance: $_scope',
          style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.8)),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: Text('Cancelar', style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6))),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: Theme.of(context).colorScheme.primary,
              foregroundColor: Colors.white,
            ),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('SÍ, BLOQUEAR'),
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
      SnackBar(content: Text(msg), backgroundColor: Theme.of(context).colorScheme.primary,
          behavior: SnackBarBehavior.floating));

  void _showError(String msg) => ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg), backgroundColor: Theme.of(context).colorScheme.error,
          behavior: SnackBarBehavior.floating));

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Row(children: [
          Icon(Icons.emergency_rounded, color: primaryColor, size: 24),
          const SizedBox(width: 10),
          const Expanded(child: Text('Bloqueo de Emergencia',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold))),
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
              const _WarningBanner(
                  'Esta acción es inmediata, irreversible sin autorización, y queda registrada en el log de auditoría.'),
              const SizedBox(height: 24),
              // ID objetivo
              _buildLabel('ID del objetivo (UUID del usuario)', context),
              const SizedBox(height: 8),
              TextFormField(
                controller: _targetIdCtrl,
                style: TextStyle(color: Theme.of(context).colorScheme.onSurface),
                decoration: _inputDeco('Ej: 123e4567-e89b-12d3-a456-426614174000',
                    Icons.fingerprint_rounded, context),
                validator: (v) => v == null || v.isEmpty ? 'Campo requerido' : null,
              ),
              const SizedBox(height: 20),
              // Alcance
              _buildLabel('Alcance del bloqueo', context),
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
              _buildLabel('Motivo del bloqueo *', context),
              const SizedBox(height: 8),
              TextFormField(
                controller: _reasonCtrl,
                maxLines: 3,
                style: TextStyle(color: Theme.of(context).colorScheme.onSurface),
                decoration: _inputDeco('Describe el motivo detalladamente...',
                    Icons.description_outlined, context),
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
                    backgroundColor: primaryColor,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16)),
                    elevation: 8,
                    shadowColor: primaryColor.withOpacity(0.5),
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

  Widget _buildLabel(String text, BuildContext context) => Text(text,
      style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7), fontSize: 13, fontWeight: FontWeight.w600));

  InputDecoration _inputDeco(String hint, IconData icon, BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return InputDecoration(
        hintText: hint,
        hintStyle: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.3)),
        prefixIcon: Icon(icon, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4)),
        filled: true,
        fillColor: Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.05 : 0.02),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide.none),
        enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: BorderSide(color: Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.1 : 0.05))),
        focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: BorderSide(color: Theme.of(context).colorScheme.primary, width: 1.5)),
      );
  }
}

class _WarningBanner extends StatelessWidget {
  const _WarningBanner(this.text);
  final String text;

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).colorScheme.primary;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: primaryColor.withOpacity(isDark ? 0.15 : 0.1),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: primaryColor.withOpacity(0.4)),
      ),
      child: Row(
        children: [
          const Icon(Icons.warning_rounded, color: Color(0xFFFF6B00), size: 22),
          const SizedBox(width: 12),
          Expanded(
            child: Text(text,
                style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.85), fontSize: 12, height: 1.4)),
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
    final primaryColor = Theme.of(context).colorScheme.primary;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: selected
              ? primaryColor.withOpacity(0.2)
              : Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.04 : 0.02),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
              color: selected
                  ? primaryColor.withOpacity(0.7)
                  : Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.1 : 0.05)),
        ),
        child: Row(
          children: [
            Icon(icon,
                color: selected ? primaryColor : Theme.of(context).colorScheme.onSurface.withOpacity(0.4), size: 24),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(label,
                      style: TextStyle(
                          color: selected ? Theme.of(context).colorScheme.onSurface : Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
                          fontWeight: FontWeight.w600,
                          fontSize: 14)),
                  Text(subtitle,
                      style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4), fontSize: 11)),
                ],
              ),
            ),
            if (selected)
              Icon(Icons.check_circle_rounded, color: primaryColor, size: 22),
          ],
        ),
      ),
    );
  }
}
