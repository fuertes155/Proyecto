import 'package:intl/intl.dart';

/// Pesos colombianos como se escriben en Colombia: "$ 1.250.000" (símbolo
/// adelante, punto de miles, sin decimales). El patrón por defecto de `es_CO`
/// en `intl` pone el símbolo al final ("1.250.000 $").
final _copFormatter = NumberFormat.currency(
  locale: 'es_CO',
  symbol: r'$',
  decimalDigits: 0,
  customPattern: '¤ #,##0',
);

String formatCop(double amount) => _copFormatter.format(amount);
