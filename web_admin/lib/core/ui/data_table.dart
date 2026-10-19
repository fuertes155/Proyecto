import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

/// Columna de [AdminTable].
class AdminColumn<T> {
  const AdminColumn({
    required this.label,
    required this.cell,
    this.flex = 2,
    this.alignEnd = false,
    this.showOnCompact = true,
  });

  final String label;
  final Widget Function(T row) cell;
  final int flex;
  final bool alignEnd;

  /// Si es false, la columna se omite en la vista de tarjetas (pantallas angostas).
  final bool showOnCompact;
}

/// Tabla del panel: en pantallas anchas, filas con encabezado; en angostas,
/// cada fila se muestra como tarjeta con pares etiqueta/valor.
class AdminTable<T> extends StatelessWidget {
  const AdminTable({
    super.key,
    required this.columns,
    required this.rows,
    this.compactBreakpoint = 760,
    this.footer,
  });

  final List<AdminColumn<T>> columns;
  final List<T> rows;
  final double compactBreakpoint;
  final Widget? footer;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(builder: (context, c) {
      return c.maxWidth >= compactBreakpoint ? _buildTable() : _buildCards();
    });
  }

  Widget _buildTable() {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            color: MetColors.subtle,
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
            child: Row(
              children: [
                for (final col in columns)
                  Expanded(
                    flex: col.flex,
                    child: Text(
                      col.label.toUpperCase(),
                      textAlign: col.alignEnd ? TextAlign.right : TextAlign.left,
                      style: const TextStyle(
                        fontSize: 11.5,
                        fontWeight: FontWeight.w700,
                        letterSpacing: 0.8,
                        color: MetColors.textMuted,
                      ),
                    ),
                  ),
              ],
            ),
          ),
          const Divider(),
          for (var i = 0; i < rows.length; i++) ...[
            _HoverRow(
              child: Row(
                children: [
                  for (final col in columns)
                    Expanded(
                      flex: col.flex,
                      child: Align(
                        alignment: col.alignEnd ? Alignment.centerRight : Alignment.centerLeft,
                        child: col.cell(rows[i]),
                      ),
                    ),
                ],
              ),
            ),
            if (i < rows.length - 1) const Divider(),
          ],
          if (footer != null) ...[const Divider(), footer!],
        ],
      ),
    );
  }

  Widget _buildCards() {
    final visible = columns.where((c) => c.showOnCompact).toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (final row in rows)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    if (visible.isNotEmpty) visible.first.cell(row),
                    for (final col in visible.skip(1)) ...[
                      const SizedBox(height: 10),
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.center,
                        children: [
                          SizedBox(
                            width: 110,
                            child: Text(col.label,
                                style: const TextStyle(fontSize: 13, color: MetColors.textMuted)),
                          ),
                          Expanded(
                            child: Align(alignment: Alignment.centerLeft, child: col.cell(row)),
                          ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        if (footer != null) Card(child: footer!),
      ],
    );
  }
}

class _HoverRow extends StatefulWidget {
  const _HoverRow({required this.child});

  final Widget child;

  @override
  State<_HoverRow> createState() => _HoverRowState();
}

class _HoverRowState extends State<_HoverRow> {
  bool _hover = false;

  @override
  Widget build(BuildContext context) {
    return MouseRegion(
      onEnter: (_) => setState(() => _hover = true),
      onExit: (_) => setState(() => _hover = false),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 120),
        color: _hover ? MetColors.subtle : Colors.transparent,
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
        child: widget.child,
      ),
    );
  }
}

/// Texto principal + secundario apilados (p. ej. nombre y correo en una celda).
class TwoLineCell extends StatelessWidget {
  const TwoLineCell(this.primary, this.secondary, {super.key, this.leading, this.monospaceSecondary = false});

  final String primary;
  final String? secondary;
  final Widget? leading;
  final bool monospaceSecondary;

  @override
  Widget build(BuildContext context) {
    final texts = Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(primary,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontSize: 14.5, fontWeight: FontWeight.w600, color: MetColors.text)),
        if (secondary != null && secondary!.isNotEmpty)
          Text(secondary!,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 13,
                color: MetColors.textMuted,
                fontFamily: monospaceSecondary ? 'monospace' : null,
              )),
      ],
    );
    if (leading == null) return texts;
    return Row(
      children: [leading!, const SizedBox(width: 12), Flexible(child: texts)],
    );
  }
}

class CellText extends StatelessWidget {
  const CellText(this.text, {super.key, this.strong = false, this.muted = false});

  final String text;
  final bool strong;
  final bool muted;

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      maxLines: 2,
      overflow: TextOverflow.ellipsis,
      style: TextStyle(
        fontSize: 14,
        fontWeight: strong ? FontWeight.w700 : FontWeight.w500,
        color: muted ? MetColors.textMuted : MetColors.text,
      ),
    );
  }
}
