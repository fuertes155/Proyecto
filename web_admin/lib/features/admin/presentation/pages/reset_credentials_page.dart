import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/ui/ui.dart';
import '../../data/repositories/admin_repository.dart';
import '../shell/admin_nav.dart';
import '../widgets/critical_action_layout.dart';
import '../widgets/user_picker_field.dart';

class ResetCredentialsPage extends ConsumerStatefulWidget {
  const ResetCredentialsPage({super.key});

  @override
  ConsumerState<ResetCredentialsPage> createState() => _ResetCredentialsPageState();
}

class _ResetCredentialsPageState extends ConsumerState<ResetCredentialsPage> {
  final _formKey = GlobalKey<FormState>();
  final _userIdCtrl = TextEditingController();
  final _reasonCtrl = TextEditingController();
  bool _isLoading = false;
  int _formVersion = 0;

  @override
  void dispose() {
    _userIdCtrl.dispose();
    _reasonCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final confirmed = await confirmAction(
      context,
      title: '¿Resetear credenciales?',
      message: 'El socio tendrá que volver a registrar su PIN y su biometría la próxima vez que entre a la app.',
      confirmLabel: 'Resetear credenciales',
      destructive: true,
      icon: Icons.lock_reset_rounded,
      details: [('Usuario', _userIdCtrl.text.trim()), ('Motivo', _reasonCtrl.text.trim())],
    );
    if (!confirmed) return;

    setState(() => _isLoading = true);
    try {
      await ref.read(adminRepositoryProvider).resetUserCredentials(_userIdCtrl.text.trim(), _reasonCtrl.text.trim());
      if (!mounted) return;
      showMetSnack(context, 'Credenciales reseteadas. El socio deberá registrar un PIN nuevo.');
      _userIdCtrl.clear();
      _reasonCtrl.clear();
      setState(() => _formVersion++);
    } catch (e) {
      if (mounted) showErrorSnack(context, e);
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AdminPage(
      section: sectionTitleFor(resetCredentialsItem.route),
      title: 'Reseteo de credenciales',
      subtitle: 'Para socios que olvidaron su PIN, cambiaron de celular o reportan que alguien más conoce su clave.',
      maxWidth: 1120,
      children: [
        CriticalActionLayout(
          form: SectionCard(
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const FieldLabel('Socio', required: true),
                  UserPickerField(key: ValueKey(_formVersion), idController: _userIdCtrl, enabled: !_isLoading),
                  const SizedBox(height: 20),
                  const FieldLabel('Motivo', required: true, help: 'Cómo lo solicitó el socio y cómo validaste su identidad.'),
                  TextFormField(
                    controller: _reasonCtrl,
                    enabled: !_isLoading,
                    maxLines: 3,
                    maxLength: 500,
                    decoration: const InputDecoration(
                      hintText: 'Ej.: Solicitud por pérdida del celular, identidad validada con videollamada.',
                    ),
                    validator: (v) => (v == null || v.trim().length < 10) ? 'Describe el motivo (mínimo 10 caracteres)' : null,
                  ),
                  const SizedBox(height: 12),
                  DangerSubmitButton(
                    label: 'Resetear credenciales',
                    loadingLabel: 'Reseteando…',
                    icon: Icons.lock_reset_rounded,
                    loading: _isLoading,
                    onPressed: _submit,
                  ),
                ],
              ),
            ),
          ),
          aside: const ConsequencesCard(items: [
            (Icons.logout_rounded, 'Se cierran todas sus sesiones guardadas en cualquier dispositivo.'),
            (Icons.pin_outlined, 'Se borra su PIN: al volver a entrar deberá crear uno nuevo.'),
            (Icons.fingerprint_rounded, 'Se borra su registro biométrico y tendrá que activarlo otra vez.'),
            (Icons.account_balance_wallet_outlined, 'Su saldo, cuentas y datos personales no cambian.'),
          ]),
        ),
      ],
    );
  }
}
