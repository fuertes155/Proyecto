/// Instrumento de inversión disponible en la plataforma.
class InvestmentInstrument {
  InvestmentInstrument({
    required this.id,
    required this.nombre,
    this.descripcion,
    required this.tasaAnual,
    required this.plazoDias,
    required this.montoMinimo,
    this.cupoMaximo,
    required this.activo,
  });

  factory InvestmentInstrument.fromJson(Map<String, dynamic> json) {
    return InvestmentInstrument(
      id: json['id'] as String,
      nombre: json['nombre'] as String,
      descripcion: json['descripcion'] as String?,
      tasaAnual: (json['tasaAnual'] as num).toDouble(),
      plazoDias: json['plazoDias'] as int,
      montoMinimo: (json['montoMinimo'] as num).toDouble(),
      cupoMaximo: (json['cupoMaximo'] as num?)?.toDouble(),
      activo: json['activo'] as bool,
    );
  }

  final String id;
  final String nombre;
  final String? descripcion;

  /// Tasa anual (ej: 0.085 = 8.5%)
  final double tasaAnual;
  final int plazoDias;
  final double montoMinimo;
  final double? cupoMaximo;
  final bool activo;

  /// Tasa en porcentaje para mostrar al usuario (ej: "8.50%")
  String get tasaLabel => '${(tasaAnual * 100).toStringAsFixed(2)}%';

  /// Plazo en formato legible
  String get plazoLabel {
    if (plazoDias >= 365) return '${(plazoDias / 365).round()} año(s)';
    if (plazoDias >= 30) return '${(plazoDias / 30).round()} mes(es)';
    return '$plazoDias días';
  }
}

/// Portfolio de micro-inversiones del usuario.
class InvestmentPortfolio {
  InvestmentPortfolio({
    required this.id,
    required this.montoTotal,
    required this.estrategia,
    required this.estado,
    required this.rendimientoTotalProyectado,
    required this.totalAlVencer,
    required this.createdAt,
    required this.posiciones,
  });

  factory InvestmentPortfolio.fromJson(Map<String, dynamic> json) {
    final posicionesJson = json['posiciones'] as List<dynamic>? ?? [];
    return InvestmentPortfolio(
      id: json['id'] as String,
      montoTotal: (json['montoTotal'] as num).toDouble(),
      estrategia: json['estrategia'] as String,
      estado: json['estado'] as String,
      rendimientoTotalProyectado:
          (json['rendimientoTotalProyectado'] as num).toDouble(),
      totalAlVencer: (json['totalAlVencer'] as num).toDouble(),
      createdAt: json['createdAt'] as String,
      posiciones: posicionesJson
          .map((e) =>
              InvestmentPosition.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  final String id;
  final double montoTotal;
  final String estrategia;
  final String estado;
  final double rendimientoTotalProyectado;
  final double totalAlVencer;
  final String createdAt;
  final List<InvestmentPosition> posiciones;

  String get estrategiaLabel => switch (estrategia) {
        'EQUAL' => 'Igualitaria',
        'WEIGHTED' => 'Ponderada',
        'RISK_BASED' => 'Por Riesgo',
        _ => estrategia,
      };

  String get estadoLabel => switch (estado) {
        'ACTIVE' => 'Activo',
        'COMPLETED' => 'Completado',
        'CANCELLED' => 'Cancelado',
        _ => estado,
      };

  double get rendimientoPercentage =>
      montoTotal > 0 ? (rendimientoTotalProyectado / montoTotal * 100) : 0;
}

/// Posición individual de inversión dentro de un portfolio.
class InvestmentPosition {
  InvestmentPosition({
    required this.id,
    required this.instrumentNombre,
    required this.montoInvertido,
    required this.tasaAplicada,
    required this.plazoDias,
    required this.fechaInicio,
    required this.fechaVencimiento,
    required this.rendimientoProyectado,
    required this.totalAlVencer,
    required this.estado,
  });

  factory InvestmentPosition.fromJson(Map<String, dynamic> json) {
    return InvestmentPosition(
      id: json['id'] as String,
      instrumentNombre: json['instrumentNombre'] as String,
      montoInvertido: (json['montoInvertido'] as num).toDouble(),
      tasaAplicada: (json['tasaAplicada'] as num).toDouble(),
      plazoDias: json['plazoDias'] as int,
      fechaInicio: json['fechaInicio'] as String,
      fechaVencimiento: json['fechaVencimiento'] as String,
      rendimientoProyectado: (json['rendimientoProyectado'] as num).toDouble(),
      totalAlVencer: (json['totalAlVencer'] as num).toDouble(),
      estado: json['estado'] as String,
    );
  }

  final String id;
  final String instrumentNombre;
  final double montoInvertido;
  final double tasaAplicada;
  final int plazoDias;
  final String fechaInicio;
  final String fechaVencimiento;
  final double rendimientoProyectado;
  final double totalAlVencer;
  final String estado;
}

/// Rendimiento cobrado al vencer una inversión.
class InvestmentReturn {
  InvestmentReturn({
    required this.id,
    required this.capital,
    required this.rendimiento,
    required this.totalAcreditado,
    required this.fechaPago,
  });

  factory InvestmentReturn.fromJson(Map<String, dynamic> json) {
    return InvestmentReturn(
      id: json['id'] as String,
      capital: (json['capital'] as num).toDouble(),
      rendimiento: (json['rendimiento'] as num).toDouble(),
      totalAcreditado: (json['totalAcreditado'] as num).toDouble(),
      fechaPago: json['fechaPago'] as String,
    );
  }

  final String id;
  final double capital;
  final double rendimiento;
  final double totalAcreditado;
  final String fechaPago;
}

/// Request para crear un portfolio.
class CreatePortfolioRequest {
  CreatePortfolioRequest({required this.montoTotal, this.estrategia = 'EQUAL'});

  final double montoTotal;
  final String estrategia;

  Map<String, dynamic> toJson() => {
        'montoTotal': montoTotal,
        'estrategia': estrategia,
      };
}
