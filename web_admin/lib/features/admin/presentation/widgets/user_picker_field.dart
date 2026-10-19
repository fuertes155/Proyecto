import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/ui/ui.dart';
import '../../providers/admin_business_provider.dart';

final _uuidPattern = RegExp(r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$');

/// Selector de socio para acciones administrativas: busca por nombre,
/// documento o correo y entrega el UUID en [idController]. También acepta
/// pegar un UUID directamente (por si la lista de socios no carga).
class UserPickerField extends ConsumerStatefulWidget {
  const UserPickerField({super.key, required this.idController, this.enabled = true});

  final TextEditingController idController;
  final bool enabled;

  @override
  ConsumerState<UserPickerField> createState() => _UserPickerFieldState();
}

class _UserPickerFieldState extends ConsumerState<UserPickerField> {
  UserModel? _selected;
  TextEditingController? _textController;

  void _onTextChanged(String text) {
    final trimmed = text.trim();
    setState(() {
      if (_selected != null && text != _label(_selected!)) _selected = null;
      if (_selected == null) {
        widget.idController.text = _uuidPattern.hasMatch(trimmed) ? trimmed : '';
      }
    });
  }

  static String _label(UserModel u) => '${u.fullName} · ${u.documentLabel}';

  void _clear() {
    setState(() => _selected = null);
    widget.idController.clear();
    _textController?.clear();
  }

  @override
  Widget build(BuildContext context) {
    final users = ref.watch(usersProvider).valueOrNull ?? const <UserModel>[];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Autocomplete<UserModel>(
          displayStringForOption: _label,
          optionsBuilder: (value) {
            final q = value.text.trim().toLowerCase();
            if (q.length < 2) return const Iterable<UserModel>.empty();
            return users
                .where((u) =>
                    '${u.fullName} ${u.documentNumber} ${u.email}'.toLowerCase().contains(q) ||
                    u.id.toLowerCase().startsWith(q))
                .take(8);
          },
          onSelected: (u) {
            setState(() => _selected = u);
            widget.idController.text = u.id;
          },
          fieldViewBuilder: (context, controller, focusNode, onSubmit) {
            _textController = controller;
            return TextFormField(
              controller: controller,
              focusNode: focusNode,
              enabled: widget.enabled,
              onChanged: _onTextChanged,
              onFieldSubmitted: (_) => onSubmit(),
              decoration: InputDecoration(
                hintText: 'Busca por nombre, documento o correo',
                prefixIcon: const Icon(Icons.person_search_rounded),
                suffixIcon: controller.text.isEmpty
                    ? null
                    : IconButton(tooltip: 'Limpiar', icon: const Icon(Icons.close_rounded, size: 20), onPressed: _clear),
              ),
              validator: (_) => widget.idController.text.isEmpty
                  ? 'Elige un socio de la lista o pega su ID (UUID)'
                  : null,
            );
          },
          optionsViewBuilder: (context, onSelected, options) => Align(
            alignment: Alignment.topLeft,
            child: Material(
              elevation: 0,
              color: Colors.transparent,
              child: Container(
                margin: const EdgeInsets.only(top: 6),
                constraints: const BoxConstraints(maxHeight: 320, maxWidth: 560),
                decoration: BoxDecoration(
                  color: MetColors.surface,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: MetColors.border),
                  boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.08), blurRadius: 24, offset: const Offset(0, 10))],
                ),
                child: ListView(
                  padding: const EdgeInsets.symmetric(vertical: 6),
                  shrinkWrap: true,
                  children: [
                    for (final u in options)
                      InkWell(
                        onTap: () => onSelected(u),
                        child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                          child: Row(
                            children: [
                              Expanded(
                                child: TwoLineCell(u.fullName, '${u.documentLabel}${u.email.isEmpty ? '' : ' · ${u.email}'}',
                                    leading: InitialsAvatar(u.fullName, size: 32)),
                              ),
                              const SizedBox(width: 10),
                              StatusBadge(kycStatus(u.kycStatus)),
                            ],
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
        ),
        if (_selected != null) ...[
          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: MetColors.successBg,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: MetColors.primary.withValues(alpha: 0.25)),
            ),
            child: Row(
              children: [
                InitialsAvatar(_selected!.fullName, size: 34),
                const SizedBox(width: 12),
                Expanded(
                  child: TwoLineCell(_selected!.fullName, 'ID ${_selected!.id}', monospaceSecondary: true),
                ),
                const Icon(Icons.check_circle_rounded, color: MetColors.primary),
              ],
            ),
          ),
        ] else if (widget.idController.text.isNotEmpty) ...[
          const SizedBox(height: 8),
          const Text('ID válido. No se encontró en la lista, verifica que sea el socio correcto.',
              style: TextStyle(fontSize: 12.5, color: MetColors.warning)),
        ],
      ],
    );
  }
}
