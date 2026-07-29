import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/widgets/accessible_button.dart';
import '../../data/datasources/compliance_remote_datasource.dart';
import '../../data/models/compliance_models.dart';
import '../providers/compliance_provider.dart';

class RegulatoryReportsPage extends ConsumerStatefulWidget {
  const RegulatoryReportsPage({super.key});

  @override
  ConsumerState<RegulatoryReportsPage> createState() => _RegulatoryReportsPageState();
}

class _RegulatoryReportsPageState extends ConsumerState<RegulatoryReportsPage> {
  String? _selectedType;
  int _year = DateTime.now().year;
  int _month = DateTime.now().month;
  bool _isGenerating = false;

  String _getFriendlyError(Object error) {
    final msg = error.toString();
    if (msg.contains('401')) return 'Tu sesión expiró. Vuelve a iniciar sesión.';
    if (msg.contains('403')) return 'No tienes permisos administrativos para acceder a los reportes.';
    if (msg.contains('SocketException') || msg.contains('Connection refused')) return 'No hay conexión con el servidor.';
    return 'Ocurrió un error inesperado. Intenta de nuevo.';
  }

  Widget _buildRetryableError(Object error, VoidCallback onRetry) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Column(
        children: [
          Text(
            _getFriendlyError(error),
            textAlign: TextAlign.center,
            style: const TextStyle(color: Colors.red),
          ),
          const SizedBox(height: 8),
          TextButton.icon(
            onPressed: onRetry,
            icon: const Icon(Icons.refresh),
            label: const Text('Reintentar'),
          ),
        ],
      ),
    );
  }

  Future<void> _generate() async {
    if (_selectedType == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Selecciona un tipo de reporte')),
      );
      return;
    }
    setState(() => _isGenerating = true);
    try {
      await ref.read(complianceRemoteDataSourceProvider).generateReport(
            GenerateReportRequest(
              reportType: _selectedType!,
              periodYear: _year,
              periodMonth: _month,
            ),
          );
      ref.invalidate(regulatoryReportsProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Reporte generado correctamente')),
        );
      }
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(_getFriendlyError(e))));
    } finally {
      if (mounted) setState(() => _isGenerating = false);
    }
  }

  Future<void> _download(RegulatoryReport report) async {
    try {
      final bytes = await ref.read(complianceRemoteDataSourceProvider).downloadReport(report.id);
      if (!mounted) return;
      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          title: const Text('Descarga completada'),
          content: Text(
            'Archivo: ${report.fileName}\n'
            'Registros: ${report.recordCount}\n'
            'Tamaño: ${bytes.length} bytes\n'
            'SHA-256: ${report.checksumSha256 ?? 'N/A'}',
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cerrar')),
          ],
        ),
      );
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(_getFriendlyError(e))));
    }
  }

  @override
  Widget build(BuildContext context) {
    final reportsAsync = ref.watch(regulatoryReportsProvider);
    final typesAsync = ref.watch(reportTypesProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Reportes Supersolidaria')),
      body: RefreshIndicator(
        onRefresh: () async => ref.invalidate(regulatoryReportsProvider),
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            const Text(
              'Generar archivo plano',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 16),
            typesAsync.when(
              loading: () => const CircularProgressIndicator(),
              error: (e, _) => _buildRetryableError(e, () => ref.invalidate(reportTypesProvider)),
              data: (types) => DropdownButtonFormField<String>(
                value: _selectedType,
                isExpanded: true,
                decoration: const InputDecoration(labelText: 'Tipo de reporte'),
                items: types
                    .map((t) => DropdownMenuItem(value: t.code, child: Text(t.description)))
                    .toList(),
                onChanged: (v) => setState(() => _selectedType = v),
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: DropdownButtonFormField<int>(
                    value: _month,
                    decoration: const InputDecoration(labelText: 'Mes'),
                    items: List.generate(
                      12,
                      (i) => DropdownMenuItem(value: i + 1, child: Text('${i + 1}')),
                    ),
                    onChanged: (v) => setState(() => _month = v ?? _month),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: DropdownButtonFormField<int>(
                    value: _year,
                    decoration: const InputDecoration(labelText: 'Año'),
                    items: [2024, 2025, 2026]
                        .map((y) => DropdownMenuItem(value: y, child: Text('$y')))
                        .toList(),
                    onChanged: (v) => setState(() => _year = v ?? _year),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            AccessibleButton(
              label: 'Generar reporte',
              isLoading: _isGenerating,
              onPressed: _generate,
            ),
            const SizedBox(height: 32),
            const Text(
              'Reportes generados',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 12),
            reportsAsync.when(
              loading: () => const CircularProgressIndicator(),
              error: (e, _) => _buildRetryableError(e, () => ref.invalidate(regulatoryReportsProvider)),
              data: (reports) {
                if (reports.isEmpty) {
                  return const Text('No hay reportes generados');
                }
                return Column(
                  children: reports
                      .map(
                        (r) => Card(
                          child: ListTile(
                            title: Text('${r.typeLabel} · ${r.periodLabel}'),
                            subtitle: Text('${r.statusLabel} · ${r.recordCount} registros'),
                            trailing: r.status == 'COMPLETED'
                                ? IconButton(
                                    icon: const Icon(Icons.download),
                                    tooltip: 'Descargar archivo plano',
                                    onPressed: () => _download(r),
                                  )
                                : null,
                          ),
                        ),
                      )
                      .toList(),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}
