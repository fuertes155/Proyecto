import 'package:flutter_test/flutter_test.dart';
import 'package:met_admin/core/utils/currency_formatter.dart';

/// Compara ignorando el tipo exacto de espacio: el formato separa el símbolo del monto con un
/// espacio de no-separación (U+00A0) para que "\$ 1.000.000" nunca se parta en dos líneas.
String _collapseWhitespace(String s) => s.replaceAll(RegExp(r'\s+'), ' ');

void main() {
  group('formatCop', () {
    test('formatea montos enteros con separador de miles y sin decimales', () {
      expect(_collapseWhitespace(formatCop(1000000)), '\$ 1.000.000');
    });

    test('redondea a 0 decimales', () {
      expect(_collapseWhitespace(formatCop(1500.75)), '\$ 1.501');
    });

    test('formatea cero', () {
      expect(_collapseWhitespace(formatCop(0)), '\$ 0');
    });

    test('formatea negativos (ej. reversas/ajustes contables)', () {
      expect(_collapseWhitespace(formatCop(-250000)), '-\$ 250.000');
    });
  });
}
