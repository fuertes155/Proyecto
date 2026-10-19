import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../theme/app_theme.dart';
import 'status.dart';

/// Estructura común de todas las pantallas del panel: encabezado + contenido
/// centrado con ancho máximo, para que en monitores grandes no se estire.
class AdminPage extends StatelessWidget {
  const AdminPage({
    super.key,
    required this.title,
    this.subtitle,
    this.section,
    this.actions = const [],
    required this.children,
    this.maxWidth = 1240,
  });

  final String title;
  final String? subtitle;

  /// Grupo del menú al que pertenece la pantalla (se muestra sobre el título).
  final String? section;
  final List<Widget> actions;
  final List<Widget> children;
  final double maxWidth;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final narrow = constraints.maxWidth < 700;
        final hPad = narrow ? 16.0 : 32.0;
        final extra = math.max(0.0, (constraints.maxWidth - maxWidth - hPad * 2) / 2);
        return ListView(
          padding: EdgeInsets.fromLTRB(hPad + extra, narrow ? 20 : 32, hPad + extra, 56),
          children: [
            PageHeader(title: title, subtitle: subtitle, section: section, actions: actions),
            SizedBox(height: narrow ? 20 : 28),
            ...children,
          ],
        );
      },
    );
  }
}

class PageHeader extends StatelessWidget {
  const PageHeader({super.key, required this.title, this.subtitle, this.section, this.actions = const []});

  final String title;
  final String? subtitle;
  final String? section;
  final List<Widget> actions;

  @override
  Widget build(BuildContext context) {
    final texts = Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (section != null) ...[
          Text(section!.toUpperCase(),
              style: const TextStyle(
                  fontSize: 12, fontWeight: FontWeight.w700, letterSpacing: 1.1, color: MetColors.primary)),
          const SizedBox(height: 6),
        ],
        Text(title, style: Theme.of(context).textTheme.headlineMedium),
        if (subtitle != null) ...[
          const SizedBox(height: 6),
          Text(subtitle!, style: const TextStyle(fontSize: 15, height: 1.5, color: MetColors.textMuted)),
        ],
      ],
    );
    if (actions.isEmpty) return texts;
    return LayoutBuilder(builder: (context, c) {
      final actionBar = Wrap(spacing: 10, runSpacing: 10, children: actions);
      if (c.maxWidth < 760) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [texts, const SizedBox(height: 16), actionBar],
        );
      }
      return Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [Expanded(child: texts), const SizedBox(width: 24), actionBar],
      );
    });
  }
}

/// Tarjeta blanca con borde suave; opcionalmente con título y acción.
class SectionCard extends StatelessWidget {
  const SectionCard({
    super.key,
    this.title,
    this.subtitle,
    this.trailing,
    required this.child,
    this.padding = const EdgeInsets.all(24),
  });

  final String? title;
  final String? subtitle;
  final Widget? trailing;
  final Widget child;
  final EdgeInsetsGeometry padding;

  @override
  Widget build(BuildContext context) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: padding,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          mainAxisSize: MainAxisSize.min,
          children: [
            if (title != null) ...[
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(title!, style: Theme.of(context).textTheme.titleMedium),
                        if (subtitle != null) ...[
                          const SizedBox(height: 4),
                          Text(subtitle!, style: const TextStyle(fontSize: 13.5, color: MetColors.textMuted)),
                        ],
                      ],
                    ),
                  ),
                  if (trailing != null) ...[const SizedBox(width: 12), trailing!],
                ],
              ),
              const SizedBox(height: 18),
            ],
            child,
          ],
        ),
      ),
    );
  }
}

/// Recuadro de aviso (información, advertencia o peligro) dentro de una página.
class CalloutBanner extends StatelessWidget {
  const CalloutBanner({super.key, required this.message, this.title, this.tone = Tone.info, this.icon});

  final String message;
  final String? title;
  final Tone tone;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: tone.background,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: tone.foreground.withValues(alpha: 0.25)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon ?? _defaultIcon, color: tone.foreground, size: 22),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (title != null) ...[
                  Text(title!,
                      style: TextStyle(fontWeight: FontWeight.w700, fontSize: 14.5, color: tone.foreground)),
                  const SizedBox(height: 3),
                ],
                Text(message, style: const TextStyle(fontSize: 14, height: 1.5, color: MetColors.text)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  IconData get _defaultIcon => switch (tone) {
        Tone.danger => Icons.gpp_maybe_rounded,
        Tone.warning => Icons.warning_amber_rounded,
        Tone.success => Icons.verified_rounded,
        _ => Icons.info_outline_rounded,
      };
}

/// Cuadro de ícono con fondo suave, usado en tarjetas y encabezados.
class IconBadge extends StatelessWidget {
  const IconBadge(this.icon, {super.key, this.tone = Tone.success, this.size = 44});

  final IconData icon;
  final Tone tone;
  final double size;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(color: tone.background, borderRadius: BorderRadius.circular(size * 0.3)),
      child: Icon(icon, color: tone.foreground, size: size * 0.5),
    );
  }
}

/// Indicador numérico del inicio (KPI).
class StatCard extends StatelessWidget {
  const StatCard({
    super.key,
    required this.icon,
    required this.label,
    this.value,
    this.hint,
    this.tone = Tone.success,
    this.loading = false,
    this.failed = false,
    this.onTap,
  });

  final IconData icon;
  final String label;
  final String? value;
  final String? hint;
  final Tone tone;
  final bool loading;
  final bool failed;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    Widget valueWidget;
    if (loading) {
      valueWidget = Container(
        height: 30,
        width: 120,
        margin: const EdgeInsets.symmetric(vertical: 2),
        decoration: BoxDecoration(color: MetColors.neutralBg, borderRadius: BorderRadius.circular(8)),
      );
    } else if (failed) {
      valueWidget = const Text('No disponible',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: MetColors.textFaint));
    } else {
      valueWidget = FittedBox(
        fit: BoxFit.scaleDown,
        alignment: Alignment.centerLeft,
        child: Text(value ?? '—',
            style: const TextStyle(
                fontSize: 28, fontWeight: FontWeight.w800, color: MetColors.text, letterSpacing: -0.5)),
      );
    }

    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        hoverColor: MetColors.subtle,
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  IconBadge(icon, tone: tone, size: 40),
                  const Spacer(),
                  if (onTap != null)
                    const Icon(Icons.arrow_outward_rounded, size: 18, color: MetColors.textFaint),
                ],
              ),
              const SizedBox(height: 16),
              Text(label, style: const TextStyle(fontSize: 13.5, color: MetColors.textMuted, fontWeight: FontWeight.w600)),
              const SizedBox(height: 4),
              valueWidget,
              if (hint != null && !loading && !failed) ...[
                const SizedBox(height: 4),
                Text(hint!,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 13, color: MetColors.textMuted)),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

/// Cuadrícula que reparte columnas según el ancho disponible.
class ResponsiveGrid extends StatelessWidget {
  const ResponsiveGrid({
    super.key,
    required this.children,
    this.minItemWidth = 240,
    this.spacing = 16,
    this.maxColumns = 4,
  });

  final List<Widget> children;
  final double minItemWidth;
  final double spacing;
  final int maxColumns;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(builder: (context, c) {
      final fit = math.max(1, math.min(maxColumns, ((c.maxWidth + spacing) / (minItemWidth + spacing)).floor()));
      // Entre las columnas que caben, la que deja menos huecos en la última fila
      // (4 tarjetas en 3 columnas dejan una sola abajo; mejor 2 × 2).
      var columns = fit;
      if (children.length > fit) {
        int empties(int cols) => (cols - children.length % cols) % cols;
        for (var cols = fit; cols >= math.max(2, fit - 1); cols--) {
          if (empties(cols) < empties(columns)) columns = cols;
        }
      }
      final itemWidth = (c.maxWidth - spacing * (columns - 1)) / columns;
      return Wrap(
        spacing: spacing,
        runSpacing: spacing,
        children: [for (final child in children) SizedBox(width: itemWidth, child: child)],
      );
    });
  }
}

/// Etiqueta sobre un campo de formulario.
class FieldLabel extends StatelessWidget {
  const FieldLabel(this.text, {super.key, this.required = false, this.help});

  final String text;
  final bool required;
  final String? help;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text.rich(TextSpan(
            text: text,
            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: MetColors.text),
            children: [
              if (required) const TextSpan(text: ' *', style: TextStyle(color: MetColors.danger)),
            ],
          )),
          if (help != null) ...[
            const SizedBox(height: 2),
            Text(help!, style: const TextStyle(fontSize: 12.5, color: MetColors.textMuted)),
          ],
        ],
      ),
    );
  }
}

/// Campo de búsqueda con botón para limpiar.
class SearchField extends StatelessWidget {
  const SearchField({super.key, required this.controller, required this.hint, required this.onChanged});

  final TextEditingController controller;
  final String hint;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<TextEditingValue>(
      valueListenable: controller,
      builder: (context, value, _) => TextField(
        controller: controller,
        onChanged: onChanged,
        decoration: InputDecoration(
          hintText: hint,
          prefixIcon: const Icon(Icons.search_rounded),
          fillColor: MetColors.surface,
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          suffixIcon: value.text.isEmpty
              ? null
              : IconButton(
                  tooltip: 'Limpiar búsqueda',
                  icon: const Icon(Icons.close_rounded, size: 20),
                  onPressed: () {
                    controller.clear();
                    onChanged('');
                  },
                ),
        ),
      ),
    );
  }
}

/// Opción de un filtro por segmentos, con contador opcional.
class FilterOption<T> {
  const FilterOption(this.value, this.label, {this.count});

  final T value;
  final String label;
  final int? count;
}

/// Fila de filtros tipo "Todos (12) · Pendientes (3) · Aprobados (9)".
class FilterBar<T> extends StatelessWidget {
  const FilterBar({super.key, required this.options, required this.selected, required this.onChanged});

  final List<FilterOption<T>> options;
  final T selected;
  final ValueChanged<T> onChanged;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        for (final option in options)
          ChoiceChip(
            selected: option.value == selected,
            onSelected: (_) => onChanged(option.value),
            labelPadding: const EdgeInsets.symmetric(horizontal: 4),
            label: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(option.label),
                if (option.count != null) ...[
                  const SizedBox(width: 6),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 1),
                    decoration: BoxDecoration(
                      color: option.value == selected ? MetColors.primary : MetColors.neutralBg,
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: Text('${option.count}',
                        style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w700,
                            color: option.value == selected ? Colors.white : MetColors.textMuted)),
                  ),
                ],
              ],
            ),
          ),
      ],
    );
  }
}

/// Círculo con las iniciales de una persona.
class InitialsAvatar extends StatelessWidget {
  const InitialsAvatar(this.name, {super.key, this.size = 36, this.dark = false});

  final String name;
  final double size;
  final bool dark;

  String get _initials {
    final parts = name.trim().split(RegExp(r'\s+')).where((p) => p.isNotEmpty).toList();
    if (parts.isEmpty) return '?';
    final first = parts.first[0];
    final last = parts.length > 1 ? parts.last[0] : '';
    return (first + last).toUpperCase();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: dark ? Colors.white.withValues(alpha: 0.14) : MetColors.successBg,
        shape: BoxShape.circle,
      ),
      child: Text(
        _initials,
        style: TextStyle(
          fontSize: size * 0.36,
          fontWeight: FontWeight.w700,
          color: dark ? Colors.white : MetColors.primary,
        ),
      ),
    );
  }
}

/// Logo de MET en cuadro redondeado.
class MetLogo extends StatelessWidget {
  const MetLogo({super.key, this.size = 40});

  final double size;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(size * 0.24),
      child: Image.asset('assets/images/logo_256.png', width: size, height: size, fit: BoxFit.cover),
    );
  }
}

/// Fila etiqueta → valor para detalles.
class InfoRow extends StatelessWidget {
  const InfoRow(this.label, this.value, {super.key, this.icon, this.emphasize = false});

  final String label;
  final String value;
  final IconData? icon;
  final bool emphasize;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 18, color: MetColors.textFaint),
            const SizedBox(width: 10),
          ],
          Expanded(
            flex: 2,
            child: Text(label, style: const TextStyle(fontSize: 14, color: MetColors.textMuted)),
          ),
          const SizedBox(width: 12),
          Expanded(
            flex: 3,
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: TextStyle(
                fontSize: emphasize ? 15.5 : 14,
                fontWeight: emphasize ? FontWeight.w800 : FontWeight.w600,
                color: MetColors.text,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Fecha ISO del backend → "17/10/2026 14:05" (hora local). Tolera nulos y números.
String formatDateTime(Object? raw, {bool withTime = true}) {
  final date = parseBackendDate(raw);
  if (date == null) return '—';
  final local = date.toLocal();
  String two(int n) => n.toString().padLeft(2, '0');
  final day = '${two(local.day)}/${two(local.month)}/${local.year}';
  return withTime ? '$day ${two(local.hour)}:${two(local.minute)}' : day;
}

DateTime? parseBackendDate(Object? raw) {
  if (raw == null) return null;
  if (raw is DateTime) return raw;
  if (raw is num) {
    // Segundos (Jackson con timestamps) o milisegundos.
    final ms = raw > 100000000000 ? raw.toInt() : (raw * 1000).toInt();
    return DateTime.fromMillisecondsSinceEpoch(ms, isUtc: true);
  }
  return DateTime.tryParse(raw.toString());
}

/// Identificadores largos (UUID) acortados para mostrarlos en tablas.
String shortId(String id) => id.length > 8 ? id.substring(0, 8).toUpperCase() : id.toUpperCase();
