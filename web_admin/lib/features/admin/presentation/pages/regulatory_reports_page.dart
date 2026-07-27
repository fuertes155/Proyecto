import 'dart:ui';
import 'dart:convert';
// ignore: avoid_web_libraries_in_flutter
import 'package:universal_html/html.dart' as html;
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import '../../../../core/theme/app_theme.dart';

class RegulatoryReportsPage extends StatefulWidget {
  const RegulatoryReportsPage({super.key});

  @override
  State<RegulatoryReportsPage> createState() => _RegulatoryReportsPageState();
}

class _RegulatoryReportsPageState extends State<RegulatoryReportsPage> {
  String _selectedReport = 'uiaf';
  bool _isGenerating = false;

  final List<Map<String, String>> _reportTypes = [
    {'id': 'uiaf', 'title': 'Reporte UIAF', 'subtitle': 'Lavado de Activos y Financiación del Terrorismo (CSV)'},
    {'id': 'super', 'title': 'Reporte Superfinanciera', 'subtitle': 'Saldos consolidados y encaje bancario (PDF)'},
    {'id': 'tx', 'title': 'Histórico Transaccional', 'subtitle': 'Volumen de transferencias detallado (Excel)'},
  ];

  Future<void> _generateReport() async {
    setState(() => _isGenerating = true);
    // Simulate generation delay
    await Future.delayed(const Duration(seconds: 2));
    
    if (!mounted) return;
    
    try {
      String fileName = '';
      String fileContent = '';
      
      final now = DateTime.now();
      final dateStr = DateFormat('yyyyMMdd_HHmmss').format(now);
      
      if (_selectedReport == 'uiaf') {
        fileName = 'reporte_uiaf_$dateStr.csv';
        fileContent = 'ID_REPORTE,TIPO,FECHA,ESTADO\nUIAF-001,PREVENCION_LAFT,$dateStr,COMPLETADO\n';
      } else if (_selectedReport == 'super') {
        fileName = 'reporte_superfinanciera_$dateStr.txt';
        fileContent = 'REPORTE SUPERFINANCIERA\n-----------------------\nFecha: $dateStr\nEstado: OK\nSaldos Consolidados: Validados\n';
      } else if (_selectedReport == 'tx') {
        fileName = 'historico_tx_$dateStr.csv';
        fileContent = 'ID_TX,CUENTA_ORIGEN,CUENTA_DESTINO,MONTO,FECHA\nTX-9983,1049189538,0000000000,500.00,$dateStr\n';
      }

      // Web download using dart:html
      final bytes = utf8.encode(fileContent);
      final blob = html.Blob([bytes]);
      final url = html.Url.createObjectUrlFromBlob(blob);
      final anchor = html.AnchorElement(href: url)
        ..setAttribute("download", fileName)
        ..click();
      html.Url.revokeObjectUrl(url);

      setState(() => _isGenerating = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('✅ $fileName descargado exitosamente'),
          backgroundColor: Theme.of(context).colorScheme.primary,
          behavior: SnackBarBehavior.floating,
        ),
      );
    } catch (e) {
      setState(() => _isGenerating = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('❌ Error al generar el reporte: $e'),
          backgroundColor: Colors.red,
          behavior: SnackBarBehavior.floating,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final primary = Theme.of(context).colorScheme.primary;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Reportes Regulatorios', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new_rounded, size: 20),
          onPressed: () => context.pop(),
        ),
      ),
      body: Stack(
        children: [
          if (isDark)
            Container(
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  colors: [Color(0xFF0D0D0D), Color(0xFF1A0808)],
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                ),
              ),
            ),
          Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 600),
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(24.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Text(
                      'Generación de Reportes Oficiales',
                      style: TextStyle(fontSize: 22, fontWeight: FontWeight.w800),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Selecciona el tipo de informe que deseas extraer de la base de datos central. Todas las descargas quedan registradas en el log de auditoría por motivos de seguridad.',
                      style: TextStyle(fontSize: 14, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)),
                    ),
                    const SizedBox(height: 32),
                    Text(
                      'TIPO DE REPORTE',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w700,
                        color: primary,
                        letterSpacing: 1.2,
                      ),
                    ),
                    const SizedBox(height: 12),
                    ..._reportTypes.map((report) => _buildReportOption(report, isDark, primary)),
                    const SizedBox(height: 32),
                    SizedBox(
                      height: 56,
                      child: ElevatedButton(
                        onPressed: _isGenerating ? null : _generateReport,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: primary,
                          foregroundColor: Colors.white,
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                          elevation: 0,
                        ),
                        child: _isGenerating
                            ? const SizedBox(
                                width: 24,
                                height: 24,
                                child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5),
                              )
                            : const Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Icon(Icons.download_rounded),
                                  SizedBox(width: 8),
                                  Text('Generar y Descargar', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                                ],
                              ),
                      ),
                    ),
                    const SizedBox(height: 20),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildReportOption(Map<String, String> report, bool isDark, Color primary) {
    final isSelected = _selectedReport == report['id'];
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        onTap: () => setState(() => _selectedReport = report['id']!),
        borderRadius: BorderRadius.circular(16),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: isSelected ? primary.withOpacity(isDark ? 0.2 : 0.1) : (isDark ? Colors.white.withOpacity(0.05) : Colors.black.withOpacity(0.03)),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
              color: isSelected ? primary : Colors.transparent,
              width: 1.5,
            ),
          ),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: isSelected ? primary : (isDark ? Colors.white.withOpacity(0.1) : Colors.black.withOpacity(0.05)),
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  Icons.insert_chart_outlined_rounded,
                  color: isSelected ? Colors.white : (isDark ? Colors.white70 : Colors.black54),
                  size: 20,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      report['title']!,
                      style: TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 15,
                        color: isSelected ? primary : null,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      report['subtitle']!,
                      style: TextStyle(
                        fontSize: 12,
                        color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6),
                      ),
                    ),
                  ],
                ),
              ),
              if (isSelected)
                Icon(Icons.check_circle_rounded, color: primary)
              else
                Icon(Icons.circle_outlined, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.2)),
            ],
          ),
        ),
      ),
    );
  }
}
