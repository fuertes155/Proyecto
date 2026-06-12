class GenerateReportRequest {
  GenerateReportRequest({
    required this.reportType,
    required this.periodYear,
    required this.periodMonth,
  });

  final String reportType;
  final int periodYear;
  final int periodMonth;

  Map<String, dynamic> toJson() => {
        'reportType': reportType,
        'periodYear': periodYear,
        'periodMonth': periodMonth,
      };
}

class ReportTypeInfo {
  ReportTypeInfo({required this.code, required this.description});

  factory ReportTypeInfo.fromJson(Map<String, dynamic> json) {
    return ReportTypeInfo(
      code: json['code'] as String,
      description: json['description'] as String,
    );
  }

  final String code;
  final String description;
}

class RegulatoryReport {
  RegulatoryReport({
    required this.id,
    required this.reportType,
    required this.periodYear,
    required this.periodMonth,
    required this.status,
    this.fileName,
    this.fileSizeBytes,
    this.recordCount = 0,
    this.checksumSha256,
    this.downloadUrl,
  });

  factory RegulatoryReport.fromJson(Map<String, dynamic> json) {
    return RegulatoryReport(
      id: json['id'] as String,
      reportType: json['reportType'] as String,
      periodYear: json['periodYear'] as int,
      periodMonth: json['periodMonth'] as int,
      status: json['status'] as String,
      fileName: json['fileName'] as String?,
      fileSizeBytes: json['fileSizeBytes'] as int?,
      recordCount: json['recordCount'] as int? ?? 0,
      checksumSha256: json['checksumSha256'] as String?,
      downloadUrl: json['downloadUrl'] as String?,
    );
  }

  final String id;
  final String reportType;
  final int periodYear;
  final int periodMonth;
  final String status;
  final String? fileName;
  final int? fileSizeBytes;
  final int recordCount;
  final String? checksumSha256;
  final String? downloadUrl;

  String get periodLabel => '$periodMonth/$periodYear';

  String get statusLabel => switch (status) {
        'COMPLETED' => 'Generado',
        'PENDING' => 'Pendiente',
        'GENERATING' => 'Generando',
        'FAILED' => 'Fallido',
        _ => status,
      };

  String get typeLabel => switch (reportType) {
        'ASOCIADOS' => 'Asociados',
        'AHORROS_PROGRAMADOS' => 'Ahorro programado',
        'CREDITO_PERSONAL' => 'Crédito personal',
        'AHORRO_SOLIDARIO' => 'Ahorro solidario',
        'SARLAFT' => 'SARLAFT',
        _ => reportType,
      };
}
