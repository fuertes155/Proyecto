// ignore: avoid_web_libraries_in_flutter
import 'package:universal_html/html.dart' as html;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../core/ui/ui.dart';
import '../../data/models/admin_models.dart';
import '../../data/repositories/admin_repository.dart';
import '../providers/admin_provider.dart';
import '../shell/admin_nav.dart';

/// Tipos que genera el backend (SupersolidariaReportType).
const _reportTypes = [
  ('ASOCIADOS', 'Asociados vinculados', 'Base de socios activos con datos de vinculación.', Icons.groups_rounded),
  ('AHORROS_PROGRAMADOS', 'Ahorro programado', 'Cartera de planes de ahorro programado.', Icons.savings_rounded),
  ('CREDITO_PERSONAL', 'Crédito personal', 'Cartera de créditos personales y su estado.', Icons.request_quote_rounded),
  ('AHORRO_SOLIDARIO', 'Ahorro solidario', 'Fondos de los grupos de ahorro solidario.', Icons.diversity_3_rounded),
  ('SARLAFT', 'Verificaciones SARLAFT', 'Resumen de cruces contra OFAC y ONU.', Icons.shield_rounded),
];

const _months = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
];

String _typeLabel(String code) =>
    _reportTypes.firstWhere((t) => t.$1 == code, orElse: () => (code, code, '', Icons.description)).$2;

StatusInfo _reportStatus(String code) => switch (code) {
      'COMPLETED' => const StatusInfo('Listo', Tone.success),
      'GENERATING' => const StatusInfo('Generando', Tone.info),
      'PENDING' => const StatusInfo('En cola', Tone.warning),
      'FAILED' => const StatusInfo('Falló', Tone.danger),
      _ => StatusInfo(code, Tone.neutral),
    };

final _thousands = NumberFormat('#,##0', 'es_CO');

String _fileSize(int? bytes) {
  if (bytes == null) return '—';
  if (bytes < 1024) return '$bytes B';
  if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
  return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
}

class RegulatoryReportsPage extends ConsumerStatefulWidget {
  const RegulatoryReportsPage({super.key});

  @override
  ConsumerState<RegulatoryReportsPage> createState() => _RegulatoryReportsPageState();
}

class _RegulatoryReportsPageState extends ConsumerState<RegulatoryReportsPage> {
  String _type = _reportTypes.first.$1;
  late int _year;
  late int _month;
  bool _generating = false;
  String? _downloadingId;

  @override
  void initState() {
    super.initState();
    // Por defecto, el mes anterior (el último periodo cerrado).
    final now = DateTime.now();
    final previous = DateTime(now.year, now.month - 1);
    _year = previous.year;
    _month = previous.month;
  }

  Future<void> _generate() async {
    setState(() => _generating = true);
    try {
      final report = await ref.read(adminRepositoryProvider).generateRegulatoryReport(_type, _year, _month);
      ref.invalidate(regulatoryReportsProvider);
      if (mounted) {
        showMetSnack(context,
            'Reporte de ${_typeLabel(_type).toLowerCase()} de ${_months[_month - 1].toLowerCase()} $_year generado'
            '${report.recordCount != null ? ' (${_thousands.format(report.recordCount)} registros)' : ''}');
      }
    } catch (e) {
      if (mounted) showErrorSnack(context, e);
    } finally {
      if (mounted) setState(() => _generating = false);
    }
  }

  Future<void> _download(RegulatoryReport report) async {
    setState(() => _downloadingId = report.id);
    try {
      final bytes = await ref.read(adminRepositoryProvider).downloadRegulatoryReport(report.id);
      final blob = html.Blob([bytes]);
      final url = html.Url.createObjectUrlFromBlob(blob);
      html.AnchorElement(href: url)
        ..setAttribute('download', report.fileName ?? 'reporte_${report.reportType.toLowerCase()}.txt')
        ..click();
      html.Url.revokeObjectUrl(url);
      if (mounted) showMetSnack(context, 'Descarga iniciada: ${report.fileName ?? 'reporte'}');
    } catch (e) {
      if (mounted) showErrorSnack(context, e);
    } finally {
      if (mounted) setState(() => _downloadingId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final reports = ref.watch(regulatoryReportsProvider);
    final now = DateTime.now();

    return AdminPage(
      section: sectionTitleFor(reportsItem.route),
      title: 'Reportes regulatorios',
      subtitle: 'Genera los archivos planos que la cooperativa entrega a la Supersolidaria y descarga los anteriores.',
      children: [
        SectionCard(
          title: '1. Elige el reporte',
          child: ResponsiveGrid(
            minItemWidth: 230,
            spacing: 12,
            maxColumns: 3,
            children: [
              for (final t in _reportTypes)
                _TypeOption(
                  title: t.$2,
                  description: t.$3,
                  icon: t.$4,
                  selected: _type == t.$1,
                  onTap: _generating ? null : () => setState(() => _type = t.$1),
                ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        SectionCard(
          title: '2. Elige el periodo y genera',
          child: Wrap(
            spacing: 16,
            runSpacing: 16,
            crossAxisAlignment: WrapCrossAlignment.end,
            children: [
              SizedBox(
                width: 200,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const FieldLabel('Mes'),
                    DropdownButtonFormField<int>(
                      initialValue: _month,
                      items: [for (var m = 1; m <= 12; m++) DropdownMenuItem(value: m, child: Text(_months[m - 1]))],
                      onChanged: _generating ? null : (v) => setState(() => _month = v ?? _month),
                    ),
                  ],
                ),
              ),
              SizedBox(
                width: 140,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const FieldLabel('Año'),
                    DropdownButtonFormField<int>(
                      initialValue: _year,
                      items: [for (var y = now.year; y >= 2020; y--) DropdownMenuItem(value: y, child: Text('$y'))],
                      onChanged: _generating ? null : (v) => setState(() => _year = v ?? _year),
                    ),
                  ],
                ),
              ),
              SizedBox(
                height: 52,
                child: ElevatedButton.icon(
                  onPressed: _generating ? null : _generate,
                  icon: _generating
                      ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                      : const Icon(Icons.auto_awesome_rounded, size: 20),
                  label: Text(_generating ? 'Generando…' : 'Generar reporte'),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 28),
        Row(
          children: [
            Expanded(child: Text('Historial', style: Theme.of(context).textTheme.titleLarge)),
            IconButton(
              tooltip: 'Actualizar',
              onPressed: () => ref.invalidate(regulatoryReportsProvider),
              icon: const Icon(Icons.refresh_rounded),
            ),
          ],
        ),
        const SizedBox(height: 12),
        reports.when(
          loading: () => const LoadingState(label: 'Cargando historial…', compact: true),
          error: (e, _) => ErrorState(error: e, onRetry: () => ref.invalidate(regulatoryReportsProvider)),
          data: (list) {
            if (list.isEmpty) {
              return const Card(
                child: EmptyState(
                  icon: Icons.folder_open_rounded,
                  title: 'Todavía no has generado reportes',
                  message: 'Los reportes que generes quedarán aquí, con su checksum, para descargarlos cuando los necesites.',
                ),
              );
            }
            return AdminTable<RegulatoryReport>(
              rows: list,
              columns: [
                AdminColumn(
                  label: 'Reporte',
                  flex: 4,
                  cell: (r) => TwoLineCell(_typeLabel(r.reportType), r.fileName,
                      leading: const IconBadge(Icons.description_rounded, size: 36)),
                ),
                AdminColumn(
                  label: 'Periodo',
                  flex: 2,
                  cell: (r) => CellText(r.periodMonth >= 1 && r.periodMonth <= 12
                      ? '${_months[r.periodMonth - 1]} ${r.periodYear}'
                      : '${r.periodYear}'),
                ),
                AdminColumn(
                  label: 'Registros',
                  flex: 2,
                  cell: (r) => CellText(
                      '${r.recordCount == null ? '—' : _thousands.format(r.recordCount)} · ${_fileSize(r.fileSizeBytes)}',
                      muted: true),
                ),
                AdminColumn(label: 'Generado', flex: 2, cell: (r) => CellText(formatDateTime(r.generatedAt), muted: true)),
                AdminColumn(label: 'Estado', flex: 2, cell: (r) => StatusBadge(_reportStatus(r.status))),
                AdminColumn(
                  label: 'Archivo',
                  flex: 2,
                  alignEnd: true,
                  cell: (r) => r.status != 'COMPLETED'
                      ? const Text('—', style: TextStyle(color: MetColors.textFaint))
                      : OutlinedButton.icon(
                          onPressed: _downloadingId == null ? () => _download(r) : null,
                          icon: _downloadingId == r.id
                              ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                              : const Icon(Icons.download_rounded, size: 18),
                          label: const Text('Descargar'),
                        ),
                ),
              ],
            );
          },
        ),
      ],
    );
  }
}

class _TypeOption extends StatelessWidget {
  const _TypeOption({
    required this.title,
    required this.description,
    required this.icon,
    required this.selected,
    required this.onTap,
  });

  final String title;
  final String description;
  final IconData icon;
  final bool selected;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: selected ? MetColors.successBg : MetColors.subtle,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 150),
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: selected ? MetColors.primary : MetColors.border, width: selected ? 1.5 : 1),
          ),
          child: Row(
            children: [
              IconBadge(icon, tone: selected ? Tone.success : Tone.neutral, size: 40),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: const TextStyle(fontSize: 14.5, fontWeight: FontWeight.w700)),
                    Text(description,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 12.5, color: MetColors.textMuted, height: 1.35)),
                  ],
                ),
              ),
              if (selected) const Icon(Icons.check_circle_rounded, color: MetColors.primary, size: 20),
            ],
          ),
        ),
      ),
    );
  }
}
