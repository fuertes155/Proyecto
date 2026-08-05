import 'package:flutter_test/flutter_test.dart';
import 'package:met_admin/core/utils/currency_formatter.dart';

/// Compara ignorando el tipo exacto de espacio: los datos CLDR para es_CO separan el símbolo
/// del monto con un espacio de no-separación (U+00A0), no un espacio normal (U+0020), y eso es
/// una decisión de localización de `intl`, no algo que este test deba fijar en piedra.
String _collapseWhitespace(String s) => s.replaceAll(RegExp(r'\s+'), ' ');

void main() {
  group('formatCop', () {
    test('formatea montos enteros con separador de miles y sin decimales', () {
      expect(_collapseWhitespace(formatCop(1000000)), '1.000.000 \$');
    });

    test('redondea a 0 decimales', () {
      expect(_collapseWhitespace(formatCop(1500.75)), '1.501 \$');
    });

    test('formatea cero', () {
      expect(_collapseWhitespace(formatCop(0)), '0 \$');
    });

    test('formatea negativos (ej. reversas/ajustes contables)', () {
      expect(_collapseWhitespace(formatCop(-250000)), '-250.000 \$');
    });
  });
}
