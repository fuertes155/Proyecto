import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/widgets/accessible_button.dart';
import '../../data/models/solidarity_models.dart';
import '../providers/solidarity_provider.dart';

class CreateSolidarityGroupPage extends ConsumerStatefulWidget {
  const CreateSolidarityGroupPage({super.key});

  @override
  ConsumerState<CreateSolidarityGroupPage> createState() => _CreateSolidarityGroupPageState();
}

class _CreateSolidarityGroupPageState extends ConsumerState<CreateSolidarityGroupPage> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _minContributionController = TextEditingController(text: '10000');
  bool _isLoading = false;

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    _minContributionController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _isLoading = true);
    try {
      final group = await ref.read(solidarityGroupsProvider.notifier).create(
            CreateSolidarityGroupRequest(
              name: _nameController.text.trim(),
              description: _descriptionController.text.trim().isEmpty
                  ? null
                  : _descriptionController.text.trim(),
              minContribution: double.parse(_minContributionController.text),
            ),
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Grupo creado. Código: ${group.inviteCode}')),
      );
      context.go('/solidarity/${group.id}');
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Crear grupo solidario')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              children: [
                TextFormField(
                  controller: _nameController,
                  decoration: const InputDecoration(labelText: 'Nombre del grupo'),
                  validator: (v) => v == null || v.isEmpty ? 'Requerido' : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _descriptionController,
                  decoration: const InputDecoration(labelText: 'Descripción (opcional)'),
                  maxLines: 2,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _minContributionController,
                  decoration: const InputDecoration(
                    labelText: 'Aporte mínimo por miembro',
                    prefixText: '\$ ',
                  ),
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  validator: (v) {
                    final val = double.tryParse(v ?? '');
                    if (val == null || val < 5000) return 'Mínimo \$5.000';
                    return null;
                  },
                ),
                const SizedBox(height: 32),
                AccessibleButton(label: 'Crear grupo', isLoading: _isLoading, onPressed: _submit),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
