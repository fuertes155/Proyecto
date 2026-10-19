import 'package:flutter/material.dart';

import '../../../../core/ui/ui.dart';

/// Estructura de las acciones críticas: formulario a la izquierda y, a la
/// derecha, qué va a pasar al ejecutarla. En pantallas angostas se apilan.
class CriticalActionLayout extends StatelessWidget {
  const CriticalActionLayout({super.key, required this.form, required this.aside});

  final Widget form;
  final Widget aside;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(builder: (context, c) {
      if (c.maxWidth < 980) {
        return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [form, const SizedBox(height: 20), aside]);
      }
      return Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(flex: 3, child: form),
          const SizedBox(width: 24),
          Expanded(flex: 2, child: aside),
        ],
      );
    });
  }
}

/// Tarjeta lateral con los efectos de la acción.
class ConsequencesCard extends StatelessWidget {
  const ConsequencesCard({super.key, required this.items, this.title = 'Qué va a pasar'});

  final String title;
  final List<(IconData, String)> items;

  @override
  Widget build(BuildContext context) {
    return SectionCard(
      title: title,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (final (icon, text) in items)
            Padding(
              padding: const EdgeInsets.only(bottom: 14),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  IconBadge(icon, tone: Tone.neutral, size: 34),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.only(top: 6),
                      child: Text(text, style: const TextStyle(fontSize: 14, height: 1.45)),
                    ),
                  ),
                ],
              ),
            ),
          const Divider(),
          const SizedBox(height: 12),
          const Row(
            children: [
              Icon(Icons.history_rounded, size: 18, color: MetColors.primary),
              SizedBox(width: 8),
              Expanded(
                child: Text('Queda registrado en la auditoría con tu usuario, la hora y tu IP.',
                    style: TextStyle(fontSize: 13, color: MetColors.textMuted)),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

/// Botón principal de una acción crítica (rojo, con estado de carga).
class DangerSubmitButton extends StatelessWidget {
  const DangerSubmitButton({
    super.key,
    required this.label,
    required this.icon,
    required this.loading,
    required this.onPressed,
    this.loadingLabel = 'Procesando…',
  });

  final String label;
  final String loadingLabel;
  final IconData icon;
  final bool loading;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 50,
      child: ElevatedButton.icon(
        onPressed: loading ? null : onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: MetColors.danger,
          disabledBackgroundColor: MetColors.danger.withValues(alpha: 0.55),
        ),
        icon: loading
            ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
            : Icon(icon, size: 20),
        label: Text(loading ? loadingLabel : label),
      ),
    );
  }
}
