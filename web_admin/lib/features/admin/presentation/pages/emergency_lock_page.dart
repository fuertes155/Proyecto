import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/ui/ui.dart';
import '../../data/models/admin_models.dart';
import '../../data/repositories/admin_repository.dart';
import '../shell/admin_nav.dart';
import '../widgets/critical_action_layout.dart';
import '../widgets/user_picker_field.dart';

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
  int _formVersion = 0;

  static const _scopes = [
    ('USER', 'Suspender la cuenta', 'Pasa a suspendida y se cierran todas sus sesiones.', Icons.person_off_rounded),
    ('ACCESS_TOKEN', 'Cerrar todas sus sesiones', 'La cuenta sigue activa; tendrá que volver a iniciar sesión.', Icons.logout_rounded),
  ];

  @override
  void dispose() {
    _targetIdCtrl.dispose();
    _reasonCtrl.dispose();
    super.dispose();
  }

  Future<void> _execute() async {
    if (!_formKey.currentState!.validate()) return;
    final scope = _scopes.firstWhere((s) => s.$1 == _scope);

    final confirmed = await confirmAction(
      context,
      title: '¿Ejecutar bloqueo de emergencia?',
      message: 'El efecto es inmediato y queda registrado en la auditoría con el motivo que escribiste.',
      confirmLabel: 'Sí, bloquear',
      destructive: true,
      icon: Icons.emergency_rounded,
      details: [
        ('Usuario', _targetIdCtrl.text.trim()),
        ('Alcance', scope.$2),
        ('Motivo', _reasonCtrl.text.trim()),
      ],
    );
    if (!confirmed) return;

    setState(() => _isLoading = true);
    try {
      await ref.read(adminRepositoryProvider).emergencyLock(EmergencyLockRequest(
            targetId: _targetIdCtrl.text.trim(),
            scope: _scope,
            reason: _reasonCtrl.text.trim(),
          ));
      if (!mounted) return;
      showMetSnack(context, 'Bloqueo de emergencia ejecutado');
      _targetIdCtrl.clear();
      _reasonCtrl.clear();
      setState(() => _formVersion++); // reinicia el buscador de socios
    } catch (e) {
      if (mounted) showErrorSnack(context, e);
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AdminPage(
      section: sectionTitleFor(emergencyLockItem.route),
      title: 'Bloqueo de emergencia',
      subtitle: 'Úsalo ante fraude, suplantación o robo de credenciales: corta el acceso de un socio al instante.',
      maxWidth: 1120,
      children: [
        CriticalActionLayout(
          form: SectionCard(
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const FieldLabel('Socio a bloquear', required: true),
                  UserPickerField(key: ValueKey(_formVersion), idController: _targetIdCtrl, enabled: !_isLoading),
                  const SizedBox(height: 22),
                  const FieldLabel('Alcance del bloqueo', required: true),
                  for (final s in _scopes)
                    _ScopeOption(
                      title: s.$2,
                      subtitle: s.$3,
                      icon: s.$4,
                      selected: _scope == s.$1,
                      onTap: _isLoading ? null : () => setState(() => _scope = s.$1),
                    ),
                  const SizedBox(height: 14),
                  const FieldLabel('Motivo', required: true, help: 'Explica qué pasó. Lo leerán auditoría y cumplimiento.'),
                  TextFormField(
                    controller: _reasonCtrl,
                    enabled: !_isLoading,
                    maxLines: 4,
                    maxLength: 500,
                    decoration: const InputDecoration(
                      hintText: 'Ej.: El socio reportó por la línea de soporte que le robaron el celular.',
                    ),
                    validator: (v) => v == null || v.trim().length < 10 ? 'Describe el motivo (mínimo 10 caracteres)' : null,
                  ),
                  const SizedBox(height: 18),
                  DangerSubmitButton(
                    label: 'Ejecutar bloqueo',
                    loadingLabel: 'Bloqueando…',
                    icon: Icons.block_rounded,
                    loading: _isLoading,
                    onPressed: _execute,
                  ),
                ],
              ),
            ),
          ),
          aside: const Column(
            children: [
              CalloutBanner(
                tone: Tone.danger,
                title: 'Acción de alto impacto',
                message: 'Se aplica de inmediato sobre la cuenta del socio. Úsala solo ante una sospecha real.',
              ),
              SizedBox(height: 16),
              ConsequencesCard(items: [
                (Icons.person_off_rounded, 'Con "Suspender la cuenta", el socio queda suspendido y no podrá volver a iniciar sesión.'),
                (Icons.phonelink_lock_rounded, 'En ambos casos se revocan sus sesiones guardadas en todos los dispositivos.'),
                (Icons.account_balance_wallet_outlined, 'Su saldo no se modifica: queda protegido mientras se investiga.'),
              ]),
            ],
          ),
        ),
      ],
    );
  }
}

class _ScopeOption extends StatelessWidget {
  const _ScopeOption({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.selected,
    required this.onTap,
  });

  final String title;
  final String subtitle;
  final IconData icon;
  final bool selected;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Material(
        color: selected ? MetColors.dangerBg : MetColors.subtle,
        borderRadius: BorderRadius.circular(14),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(14),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 150),
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(14),
              border: Border.all(
                color: selected ? MetColors.danger.withValues(alpha: 0.6) : MetColors.border,
                width: selected ? 1.5 : 1,
              ),
            ),
            child: Row(
              children: [
                Icon(icon, color: selected ? MetColors.danger : MetColors.textMuted, size: 24),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(title, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700)),
                      Text(subtitle, style: const TextStyle(fontSize: 13, color: MetColors.textMuted)),
                    ],
                  ),
                ),
                Icon(
                  selected ? Icons.radio_button_checked_rounded : Icons.radio_button_unchecked_rounded,
                  color: selected ? MetColors.danger : MetColors.textFaint,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
