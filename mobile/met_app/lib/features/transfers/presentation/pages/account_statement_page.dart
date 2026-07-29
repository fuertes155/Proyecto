import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../core/widgets/accessible_button.dart';
import '../providers/transfers_provider.dart';

class _StatementRow {
  _StatementRow({
    required this.date,
    required this.type,
    required this.description,
    required this.amount,
    required this.isCredit,
  });

  final String date;
  final String type;
  final String description;
  final double amount;
  final bool isCredit;
}

class AccountStatementPage extends ConsumerStatefulWidget {
  const AccountStatementPage({super.key});

  @override
  ConsumerState<AccountStatementPage> createState() => _AccountStatementPageState();
}

class _AccountStatementPageState extends ConsumerState<AccountStatementPage> {
  int _year = DateTime.now().year;
  int _month = DateTime.now().month;
  bool _isLoading = false;
  String? _error;
  List<_StatementRow>? _rows;

  String _friendlyError(Object error) {
    final msg = error.toString();
    if (msg.contains('401')) return 'Tu sesión expiró. Vuelve a iniciar sesión.';
    if (msg.contains('SocketException') || msg.contains('Connection refused')) {
      return 'No hay conexión con el servidor.';
    }
    return 'No se pudo generar el extracto. Intenta de nuevo.';
  }

  List<_StatementRow> _parseCsv(String csv) {
    final lines = csv.split('\n').where((l) => l.trim().isNotEmpty).toList();
    if (lines.isEmpty) return [];
    // Primera línea es el encabezado (Fecha,Tipo,Descripcion,Monto,Direccion)
    return lines.skip(1).map((line) {
      final parts = _splitCsvLine(line);
      return _StatementRow(
        date: parts.isNotEmpty ? parts[0] : '',
        type: parts.length > 1 ? parts[1] : '',
        description: parts.length > 2 ? parts[2] : '',
        amount: parts.length > 3 ? double.tryParse(parts[3]) ?? 0 : 0,
        isCredit: parts.length > 4 && parts[4] == 'CREDITO',
      );
    }).toList();
  }

  List<String> _splitCsvLine(String line) {
    final result = <String>[];
    final buffer = StringBuffer();
    bool inQuotes = false;
    for (int i = 0; i < line.length; i++) {
      final char = line[i];
      if (char == '"') {
        inQuotes = !inQuotes;
      } else if (char == ',' && !inQuotes) {
        result.add(buffer.toString());
        buffer.clear();
      } else {
        buffer.write(char);
      }
    }
    result.add(buffer.toString());
    return result;
  }

  Future<void> _generate() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final repository = ref.read(transfersRepositoryProvider);
      final csv = await repository.getStatementCsv(year: _year, month: _month);
      setState(() => _rows = _parseCsv(csv));
    } catch (e) {
      setState(() => _error = _friendlyError(e));
    } finally {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final currency = NumberFormat.currency(locale: 'es_CO', symbol: '\$', decimalDigits: 0);

    return Scaffold(
      appBar: AppBar(title: const Text('Mis movimientos')),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Text(
            'Extracto de transacciones',
            style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 8),
          Text(
            'Genera un extracto con tus depósitos y transferencias del periodo que elijas.',
            style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant),
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              Expanded(
                child: DropdownButtonFormField<int>(
                  initialValue: _month,
                  decoration: const InputDecoration(labelText: 'Mes'),
                  items: List.generate(12, (i) => DropdownMenuItem(value: i + 1, child: Text('${i + 1}'))),
                  onChanged: (v) => setState(() => _month = v ?? _month),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: DropdownButtonFormField<int>(
                  initialValue: _year,
                  decoration: const InputDecoration(labelText: 'Año'),
                  items: [DateTime.now().year - 1, DateTime.now().year]
                      .map((y) => DropdownMenuItem(value: y, child: Text('$y')))
                      .toList(),
                  onChanged: (v) => setState(() => _year = v ?? _year),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          AccessibleButton(
            label: 'Generar extracto',
            isLoading: _isLoading,
            onPressed: _generate,
          ),
          if (_error != null) ...[
            const SizedBox(height: 16),
            Text(_error!, style: const TextStyle(color: Colors.red), textAlign: TextAlign.center),
          ],
          if (_rows != null) ...[
            const SizedBox(height: 28),
            Text(
              '${_rows!.length} movimiento(s) en $_month/$_year',
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 12),
            if (_rows!.isEmpty)
              const Text('No hay movimientos en este periodo.')
            else
              ..._rows!.map((r) => Card(
                    child: ListTile(
                      leading: Icon(
                        r.isCredit ? Icons.arrow_downward : Icons.arrow_upward,
                        color: r.isCredit ? Colors.green : Colors.red,
                      ),
                      title: Text(r.description.isEmpty ? r.type : r.description),
                      subtitle: Text('${r.type} · ${r.date}'),
                      trailing: Text(
                        '${r.isCredit ? '+' : '-'}${currency.format(r.amount)}',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: r.isCredit ? Colors.green : Colors.red,
                        ),
                      ),
                    ),
                  )),
          ],
        ],
      ),
    );
  }
}
