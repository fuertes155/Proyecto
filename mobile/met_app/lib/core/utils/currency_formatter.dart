import 'package:intl/intl.dart';

final _copFormatter = NumberFormat.currency(locale: 'es_CO', symbol: '\$', decimalDigits: 0);
final _thousands = NumberFormat('#,##0', 'es_CO');

String formatCop(double amount) => _copFormatter.format(amount);

/// Pesos al estilo colombiano: "$ 2.450.000" (o "$ 38.750,50" con [cents]). Se arma a mano para
/// no depender de dónde ubique el símbolo cada versión de los datos de localización.
String formatPesos(double amount, {bool cents = false}) {
  final sign = amount < 0 ? '-' : '';
  final totalCents = (amount.abs() * 100).round();
  final whole = _thousands.format(totalCents ~/ 100);
  if (!cents) return '$sign\$ $whole';
  return '$sign\$ $whole,${(totalCents % 100).toString().padLeft(2, '0')}';
}
