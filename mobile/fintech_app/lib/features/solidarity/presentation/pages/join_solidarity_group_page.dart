import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/widgets/accessible_button.dart';
import '../../data/models/solidarity_models.dart';
import '../providers/solidarity_provider.dart';

class JoinSolidarityGroupPage extends ConsumerStatefulWidget {
  const JoinSolidarityGroupPage({super.key});

  @override
  ConsumerState<JoinSolidarityGroupPage> createState() => _JoinSolidarityGroupPageState();
}

class _JoinSolidarityGroupPageState extends ConsumerState<JoinSolidarityGroupPage> {
  final _codeController = TextEditingController();
  bool _isLoading = false;

  @override
  void dispose() {
    _codeController.dispose();
    super.dispose();
  }

  Future<void> _join() async {
    if (_codeController.text.length != 8) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('El código debe tener 8 caracteres')),
      );
      return;
    }
    setState(() => _isLoading = true);
    try {
      await ref.read(solidarityGroupsProvider.notifier).join(
            JoinSolidarityGroupRequest(inviteCode: _codeController.text.toUpperCase()),
          );
      if (!mounted) return;
      context.go('/solidarity');
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Unirme a un grupo')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            TextField(
              controller: _codeController,
              decoration: const InputDecoration(
                labelText: 'Código de invitación',
                hintText: 'Ej: ABC12XYZ',
              ),
              textCapitalization: TextCapitalization.characters,
              maxLength: 8,
              inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[A-Z0-9]'))],
            ),
            const SizedBox(height: 24),
            AccessibleButton(label: 'Unirme al grupo', isLoading: _isLoading, onPressed: _join),
          ],
        ),
      ),
    );
  }
}
