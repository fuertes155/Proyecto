import 'package:flutter/material.dart';

import '../theme/app_theme.dart';
import 'states.dart';
import 'status.dart';

/// Aviso breve en la parte inferior. [tone] define el ícono (éxito por defecto).
void showMetSnack(BuildContext context, String message, {Tone tone = Tone.success}) {
  final messenger = ScaffoldMessenger.maybeOf(context);
  if (messenger == null) return;
  final iconColor = switch (tone) {
    Tone.success => const Color(0xFF8BD18F),
    Tone.danger => const Color(0xFFFF8A80),
    Tone.warning => const Color(0xFFFFCC80),
    Tone.info => const Color(0xFF90B4FF),
    Tone.neutral => Colors.white70,
  };
  final width = MediaQuery.sizeOf(context).width;
  messenger
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(
      width: width > 560 ? 480 : null,
      duration: Duration(seconds: tone == Tone.danger ? 6 : 4),
      content: Row(
        children: [
          Icon(tone.icon, color: iconColor, size: 20),
          const SizedBox(width: 12),
          Expanded(child: Text(message)),
        ],
      ),
    ));
}

/// Muestra el error de una acción de forma legible.
void showErrorSnack(BuildContext context, Object error) =>
    showMetSnack(context, friendlyError(error), tone: Tone.danger);

/// Diálogo de confirmación. Devuelve true solo si el administrador confirma.
///
/// Con [destructive] el botón principal se pinta en rojo (bloqueos, rechazos,
/// eliminaciones, reversiones). [details] muestra pares etiqueta/valor para que
/// quede claro sobre qué se está actuando.
Future<bool> confirmAction(
  BuildContext context, {
  required String title,
  required String message,
  required String confirmLabel,
  bool destructive = false,
  IconData? icon,
  List<(String, String)> details = const [],
}) async {
  final tone = destructive ? Tone.danger : Tone.success;
  final result = await showDialog<bool>(
    context: context,
    builder: (ctx) => AlertDialog(
      titlePadding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
      contentPadding: const EdgeInsets.fromLTRB(24, 12, 24, 8),
      actionsPadding: const EdgeInsets.fromLTRB(24, 12, 24, 20),
      title: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(color: tone.background, borderRadius: BorderRadius.circular(12)),
            child: Icon(icon ?? (destructive ? Icons.warning_amber_rounded : Icons.help_outline_rounded),
                color: tone.foreground, size: 22),
          ),
          const SizedBox(width: 14),
          Expanded(child: Text(title)),
        ],
      ),
      content: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 440),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(message),
            if (details.isNotEmpty) ...[
              const SizedBox(height: 14),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: MetColors.subtle,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: MetColors.border),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    for (final (label, value) in details)
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 3),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            SizedBox(
                              width: 110,
                              child: Text(label,
                                  style: const TextStyle(fontSize: 13, color: MetColors.textMuted)),
                            ),
                            Expanded(
                              child: Text(value,
                                  style: const TextStyle(
                                      fontSize: 13.5, fontWeight: FontWeight.w600, color: MetColors.text)),
                            ),
                          ],
                        ),
                      ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(ctx).pop(false),
          style: TextButton.styleFrom(foregroundColor: MetColors.textMuted),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          onPressed: () => Navigator.of(ctx).pop(true),
          style: destructive ? FilledButton.styleFrom(backgroundColor: MetColors.danger) : null,
          child: Text(confirmLabel),
        ),
      ],
    ),
  );
  return result == true;
}
