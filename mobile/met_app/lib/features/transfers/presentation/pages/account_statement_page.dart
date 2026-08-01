import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../core/widgets/accessible_button.dart';
import '../../data/models/movement_model.dart';
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

/// Ícono y color por tipo de movimiento, para distinguir a simple vista
/// recargas, préstamos, inversiones y transferencias en el listado.
class _MovementVisual {
  const _MovementVisual(this.icon, this.color);
  final IconData icon;
  final Color color;
}

const Map<String, _MovementVisual> _movementVisuals = {
  'DEPOSIT': _MovementVisual(Icons.add_circle_outline, Color(0xFF2E7D32)),
  'WITHDRAWAL': _MovementVisual(Icons.arrow_upward, Color(0xFFC62828)),
  'TRANSFER': _MovementVisual(Icons.swap_horiz, Color(0xFF1565C0)),
  'EXTERNAL_PAYOUT': _MovementVisual(Icons.account_balance_outlined, Color(0xFFC62828)),
  'INVESTMENT_FUNDING': _MovementVisual(Icons.trending_up, Color(0xFF6A1B9A)),
  'LOAN_DISBURSEMENT': _MovementVisual(Icons.request_quote_outlined, Color(0xFF00897B)),
};

class AccountStatementPage extends ConsumerStatefulWidget {
  const AccountStatementPage({super.key});

  @override
  ConsumerState<AccountStatementPage> createState() => _AccountStatementPageState();
}

class _AccountStatementPageState extends ConsumerState<AccountStatementPage> {
  bool _isLoadingMovements = true;
  String? _movementsError;
  List<MovementModel> _movements = [];

  bool _showCsvExtract = false;
  int _year = DateTime.now().year;
  int _month = DateTime.now().month;
  bool _isLoadingCsv = false;
  String? _csvError;
  List<_StatementRow>? _csvRows;

  @override
  void initState() {
    super.initState();
    _loadMovements();
  }

  String _friendlyError(Object error) {
    final msg = error.toString();
    if (msg.contains('401')) return 'Tu sesión expiró. Vuelve a iniciar sesión.';
    if (msg.contains('SocketException') || msg.contains('Connection refused')) {
      return 'No hay conexión con el servidor.';
    }
    return 'No se pudo cargar la información. Intenta de nuevo.';
  }

  Future<void> _loadMovements() async {
    setState(() {
      _isLoadingMovements = true;
      _movementsError = null;
    });
    try {
      final repository = ref.read(transfersRepositoryProvider);
      final movements = await repository.getMovements();
      setState(() => _movements = movements);
    } catch (e) {
      setState(() => _movementsError = _friendlyError(e));
    } finally {
      setState(() => _isLoadingMovements = false);
    }
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

  Future<void> _generateCsv() async {
    setState(() {
      _isLoadingCsv = true;
      _csvError = null;
    });
    try {
      final repository = ref.read(transfersRepositoryProvider);
      final csv = await repository.getStatementCsv(year: _year, month: _month);
      setState(() => _csvRows = _parseCsv(csv));
    } catch (e) {
      setState(() => _csvError = _friendlyError(e));
    } finally {
      setState(() => _isLoadingCsv = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final currency = NumberFormat.currency(locale: 'es_CO', symbol: '\$', decimalDigits: 0);
    final dateFormat = DateFormat('dd MMM yyyy, hh:mm a', 'es_CO');

    return Scaffold(
      appBar: AppBar(title: const Text('Mis movimientos')),
      body: RefreshIndicator(
        onRefresh: _loadMovements,
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            Text(
              'Toda tu actividad',
              style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 8),
            Text(
              'Recargas, préstamos, inversiones en otros socios, transferencias y retiros, todo en un solo lugar.',
              style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 20),
            if (_isLoadingMovements)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 40),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (_movementsError != null) ...[
              Text(_movementsError!, style: const TextStyle(color: Colors.red), textAlign: TextAlign.center),
              const SizedBox(height: 12),
              AccessibleButton(label: 'Reintentar', onPressed: _loadMovements),
            ] else if (_movements.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 24),
                child: Text('Todavía no tienes movimientos registrados.'),
              )
            else
              ..._movements.map((m) {
                final visual = _movementVisuals[m.type] ??
                    const _MovementVisual(Icons.receipt_long_outlined, Colors.grey);
                return Card(
                  child: ListTile(
                    leading: CircleAvatar(
                      backgroundColor: visual.color.withOpacity(0.12),
                      child: Icon(visual.icon, color: visual.color, size: 20),
                    ),
                    title: Text(m.typeLabel),
                    subtitle: Text(
                      '${m.concept.isEmpty ? '' : '${m.concept} · '}${dateFormat.format(m.createdAt.toLocal())}',
                    ),
                    trailing: Text(
                      '${m.isCredit ? '+' : '-'}${currency.format(m.amount)}',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        color: m.isCredit ? const Color(0xFF2E7D32) : const Color(0xFFC62828),
                      ),
                    ),
                  ),
                );
              }),
            const SizedBox(height: 12),
            TextButton.icon(
              onPressed: () => setState(() => _showCsvExtract = !_showCsvExtract),
              icon: Icon(_showCsvExtract ? Icons.expand_less : Icons.expand_more),
              label: const Text('Descargar extracto mensual'),
            ),
            if (_showCsvExtract) ...[
              const Divider(height: 32),
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
                isLoading: _isLoadingCsv,
                onPressed: _generateCsv,
              ),
              if (_csvError != null) ...[
                const SizedBox(height: 16),
                Text(_csvError!, style: const TextStyle(color: Colors.red), textAlign: TextAlign.center),
              ],
              if (_csvRows != null) ...[
                const SizedBox(height: 28),
                Text(
                  '${_csvRows!.length} movimiento(s) en $_month/$_year',
                  style: const TextStyle(fontWeight: FontWeight.w600),
                ),
                const SizedBox(height: 12),
                if (_csvRows!.isEmpty)
                  const Text('No hay movimientos en este periodo.')
                else
                  ..._csvRows!.map((r) => Card(
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
          ],
        ),
      ),
    );
  }
}
