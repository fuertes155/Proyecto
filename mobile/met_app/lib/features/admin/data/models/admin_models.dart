// ============================================================
// Admin Data Models
// ============================================================

class AdminLoginRequest {
  AdminLoginRequest({required this.username, required this.password});
  final String username;
  final String password;

  Map<String, dynamic> toJson() => {
        'username': username,
        'password': password,
      };
}

class AdminAuthResponse {
  AdminAuthResponse({
    required this.adminId,
    required this.username,
    required this.fullName,
    required this.role,
    required this.accessToken,
    required this.expiresInMs,
  });

  factory AdminAuthResponse.fromJson(Map<String, dynamic> json) {
    return AdminAuthResponse(
      adminId: json['adminId'] as String,
      username: json['username'] as String,
      fullName: json['fullName'] as String,
      role: json['role'] as String,
      accessToken: json['accessToken'] as String,
      expiresInMs: json['expiresInMs'] as int,
    );
  }

  final String adminId;
  final String username;
  final String fullName;
  final String role;
  final String accessToken;
  final int expiresInMs;
}

class EmergencyLockRequest {
  EmergencyLockRequest({
    required this.targetId,
    required this.scope,
    required this.reason,
  });
  final String targetId;
  final String scope;
  final String reason;

  Map<String, dynamic> toJson() => {
        'targetId': targetId,
        'scope': scope,
        'reason': reason,
      };
}

class OperationLimit {
  OperationLimit({
    required this.tipoOperacion,
    required this.montoDiarioMax,
    required this.montoPorTransaccionMax,
    required this.activo,
  });

  factory OperationLimit.fromJson(Map<String, dynamic> json) {
    return OperationLimit(
      tipoOperacion: json['tipoOperacion'] as String,
      montoDiarioMax: (json['montoDiarioMax'] as num).toInt(),
      montoPorTransaccionMax: (json['montoPorTransaccionMax'] as num).toInt(),
      activo: json['activo'] as bool,
    );
  }

  final String tipoOperacion;
  final int montoDiarioMax;
  final int montoPorTransaccionMax;
  final bool activo;
}

class RiskRule {
  RiskRule({
    required this.id,
    required this.nombre,
    this.descripcion,
    required this.condicion,
    required this.accion,
    required this.activo,
  });

  factory RiskRule.fromJson(Map<String, dynamic> json) {
    return RiskRule(
      id: json['id'] as String,
      nombre: json['nombre'] as String,
      descripcion: json['descripcion'] as String?,
      condicion: json['condicion'] as String,
      accion: json['accion'] as String,
      activo: json['activo'] as bool,
    );
  }

  final String id;
  final String nombre;
  final String? descripcion;
  final String condicion;
  final String accion;
  final bool activo;
}

class FeeSchedule {
  FeeSchedule({
    required this.id,
    required this.tipoTarifa,
    this.descripcion,
    required this.valor,
    required this.esPorcentaje,
    required this.vigentDesde,
    this.vigentaHasta,
  });

  factory FeeSchedule.fromJson(Map<String, dynamic> json) {
    return FeeSchedule(
      id: json['id'] as String,
      tipoTarifa: json['tipoTarifa'] as String,
      descripcion: json['descripcion'] as String?,
      valor: (json['valor'] as num).toDouble(),
      esPorcentaje: json['esPorcentaje'] as bool,
      vigentDesde: json['vigentDesde'] as String,
      vigentaHasta: json['vigentaHasta'] as String?,
    );
  }

  final String id;
  final String tipoTarifa;
  final String? descripcion;
  final double valor;
  final bool esPorcentaje;
  final String vigentDesde;
  final String? vigentaHasta;
}

class MaintenanceWindow {
  MaintenanceWindow({
    required this.id,
    required this.descripcion,
    required this.inicio,
    required this.fin,
    required this.activo,
  });

  factory MaintenanceWindow.fromJson(Map<String, dynamic> json) {
    return MaintenanceWindow(
      id: json['id'] as String,
      descripcion: json['descripcion'] as String,
      inicio: json['inicio'] as String,
      fin: json['fin'] as String,
      activo: json['activo'] as bool,
    );
  }

  final String id;
  final String descripcion;
  final String inicio;
  final String fin;
  final bool activo;
}

class AuditLogEntry {
  AuditLogEntry({
    required this.id,
    required this.actorAdminId,
    required this.accion,
    this.entidadAfectada,
    this.idEntidad,
    this.valoresAnteriores,
    this.valoresNuevos,
    this.motivo,
    this.ipOrigen,
    required this.timestamp,
  });

  factory AuditLogEntry.fromJson(Map<String, dynamic> json) {
    return AuditLogEntry(
      id: json['id'] as String,
      actorAdminId: json['actorAdminId'] as String,
      accion: json['accion'] as String,
      entidadAfectada: json['entidadAfectada'] as String?,
      idEntidad: json['idEntidad'] as String?,
      valoresAnteriores: json['valoresAnteriores'] as String?,
      valoresNuevos: json['valoresNuevos'] as String?,
      motivo: json['motivo'] as String?,
      ipOrigen: json['ipOrigen'] as String?,
      timestamp: json['timestamp'] as String,
    );
  }

  final String id;
  final String actorAdminId;
  final String accion;
  final String? entidadAfectada;
  final String? idEntidad;
  final String? valoresAnteriores;
  final String? valoresNuevos;
  final String? motivo;
  final String? ipOrigen;
  final String timestamp;
}
