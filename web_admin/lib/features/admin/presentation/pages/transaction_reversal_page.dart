import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/ui/ui.dart';
import '../../data/repositories/admin_repository.dart';
import '../shell/admin_nav.dart';
import '../widgets/critical_action_layout.dart';

class TransactionReversalPage extends ConsumerStatefulWidget {
  const TransactionReversalPage({super.key});

  @override
  ConsumerState<TransactionReversalPage> createState() => _TransactionReversalPageState();
}

class _TransactionReversalPageState extends ConsumerState<TransactionReversalPage> {
  final _formKey = GlobalKey<FormState>();
  final _txIdCtrl = TextEditingController();
  final _reasonCtrl = TextEditingController();
  final _codeCtrl = TextEditingController();
  bool _isLoading = false;

  @override
  void dispose() {
    _txIdCtrl.dispose();
    _reasonCtrl.dispose();
    _codeCtrl.dispose();
    super.dispose();
  }

  /// El backend pide escribir "CONFIRM-" + los primeros 8 caracteres del ID,
  /// para que una reversión nunca se ejecute por accidente.
  String? get _expectedCode {
    final id = _txIdCtrl.text.trim();
    return id.length >= 8 ? 'CONFIRM-${id.substring(0, 8).toUpperCase()}' : null;
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final confirmed = await confirmAction(
      context,
      title: '¿Registrar la reversión?',
      message: 'La reversión quedará registrada en la auditoría con tu usuario y el motivo.',
      confirmLabel: 'Registrar reversión',
      destructive: true,
      icon: Icons.undo_rounded,
      details: [('Transacción', _txIdCtrl.text.trim()), ('Motivo', _reasonCtrl.text.trim())],
    );
    if (!confirmed) return;

    setState(() => _isLoading = true);
    try {
      await ref.read(adminRepositoryProvider).reverseTransaction(
            _txIdCtrl.text.trim(),
            _reasonCtrl.text.trim(),
            _codeCtrl.text.trim(),
          );
      if (!mounted) return;
      showMetSnack(context, 'Reversión registrada en la auditoría');
      _txIdCtrl.clear();
      _reasonCtrl.clear();
      _codeCtrl.clear();
      setState(() {});
    } catch (e) {
      if (mounted) showErrorSnack(context, e);
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final expected = _expectedCode;

    return AdminPage(
      section: sectionTitleFor(reversalItem.route),
      title: 'Reversión de transacción',
      subtitle: 'Anula una operación puntual cuando hubo un error, un doble débito o un fraude comprobado.',
      maxWidth: 1120,
      children: [
        const CalloutBanner(
          tone: Tone.warning,
          title: 'Por ahora solo se registra la solicitud',
          message: 'La plataforma deja la reversión registrada en la auditoría, pero todavía no devuelve el dinero '
              'de forma automática. El ajuste del saldo debe hacerse aparte con contabilidad.',
        ),
        const SizedBox(height: 20),
        CriticalActionLayout(
          form: SectionCard(
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const FieldLabel('ID de la transacción', required: true),
                  TextFormField(
                    controller: _txIdCtrl,
                    enabled: !_isLoading,
                    onChanged: (_) => setState(() {}),
                    style: const TextStyle(fontFamily: 'monospace'),
                    decoration: const InputDecoration(
                      hintText: 'Ej.: 7f3c9a10-1111-4c2d-9a8e-000000000001',
                      prefixIcon: Icon(Icons.receipt_long_rounded),
                    ),
                    validator: (v) => (v == null || v.trim().length < 8) ? 'Pega el ID completo de la transacción' : null,
                  ),
                  const SizedBox(height: 20),
                  const FieldLabel('Motivo', required: true),
                  TextFormField(
                    controller: _reasonCtrl,
                    enabled: !_isLoading,
                    maxLines: 3,
                    maxLength: 500,
                    decoration: const InputDecoration(hintText: 'Ej.: Doble débito por reintento de la pasarela PSE.'),
                    validator: (v) => (v == null || v.trim().length < 10) ? 'Describe el motivo (mínimo 10 caracteres)' : null,
                  ),
                  const SizedBox(height: 12),
                  FieldLabel(
                    'Confirmación',
                    required: true,
                    help: expected == null
                        ? 'Primero pega el ID de la transacción.'
                        : 'Para confirmar, escribe exactamente: $expected',
                  ),
                  TextFormField(
                    controller: _codeCtrl,
                    enabled: !_isLoading && expected != null,
                    textCapitalization: TextCapitalization.characters,
                    style: const TextStyle(fontFamily: 'monospace'),
                    decoration: InputDecoration(
                      hintText: expected ?? 'CONFIRM-XXXXXXXX',
                      prefixIcon: const Icon(Icons.keyboard_rounded),
                    ),
                    validator: (v) {
                      if (expected == null) return null;
                      return (v ?? '').trim().toUpperCase() == expected ? null : 'Escribe $expected para confirmar';
                    },
                  ),
                  const SizedBox(height: 24),
                  DangerSubmitButton(
                    label: 'Registrar reversión',
                    loadingLabel: 'Registrando…',
                    icon: Icons.undo_rounded,
                    loading: _isLoading,
                    onPressed: _submit,
                  ),
                ],
              ),
            ),
          ),
          aside: const ConsequencesCard(items: [
            (Icons.fact_check_outlined, 'Verifica antes el movimiento en el extracto del socio y en la pasarela.'),
            (Icons.keyboard_rounded, 'El código de confirmación evita reversiones por error o por copiar el ID equivocado.'),
            (Icons.account_balance_outlined, 'Coordina con contabilidad el ajuste del saldo mientras la reversión automática no esté disponible.'),
          ]),
        ),
      ],
    );
  }
}
