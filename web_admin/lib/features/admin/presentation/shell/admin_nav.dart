import 'package:flutter/material.dart';

/// Elemento del menú lateral.
class AdminNavItem {
  const AdminNavItem({
    required this.label,
    required this.icon,
    required this.route,
    required this.description,
  });

  final String label;
  final IconData icon;
  final String route;

  /// Texto corto para los accesos rápidos del inicio.
  final String description;
}

class AdminNavSection {
  const AdminNavSection(this.title, this.items);

  final String title;
  final List<AdminNavItem> items;
}

const dashboardItem = AdminNavItem(
  label: 'Inicio',
  icon: Icons.space_dashboard_rounded,
  route: '/admin/dashboard',
  description: 'Resumen general del panel',
);

const partnersItem = AdminNavItem(
  label: 'Socios y KYC',
  icon: Icons.people_alt_rounded,
  route: '/admin/partners',
  description: 'Valida la identidad de los nuevos socios',
);
const accountsItem = AdminNavItem(
  label: 'Cuentas y saldos',
  icon: Icons.account_balance_wallet_rounded,
  route: '/admin/accounts',
  description: 'Consulta cuentas, titulares y saldos',
);
const loansItem = AdminNavItem(
  label: 'Solicitudes de crédito',
  icon: Icons.request_quote_rounded,
  route: '/admin/loans',
  description: 'Aprueba o rechaza solicitudes',
);
const complianceItem = AdminNavItem(
  label: 'Cumplimiento SARLAFT',
  icon: Icons.shield_rounded,
  route: '/admin/compliance',
  description: 'Alertas y listas restrictivas',
);
const riskRulesItem = AdminNavItem(
  label: 'Reglas de riesgo',
  icon: Icons.policy_rounded,
  route: '/admin/risk-rules',
  description: 'Activa o pausa reglas antifraude',
);
const guaranteeFundItem = AdminNavItem(
  label: 'Fondo de garantías',
  icon: Icons.savings_rounded,
  route: '/admin/guarantee-fund',
  description: 'Saldo, cobertura y movimientos',
);
const limitsItem = AdminNavItem(
  label: 'Límites de operación',
  icon: Icons.tune_rounded,
  route: '/admin/limits',
  description: 'Topes diarios y por transacción',
);
const feesItem = AdminNavItem(
  label: 'Tarifas y comisiones',
  icon: Icons.percent_rounded,
  route: '/admin/fees',
  description: 'Crea y ajusta tarifas vigentes',
);
const maintenanceItem = AdminNavItem(
  label: 'Mantenimiento',
  icon: Icons.build_circle_rounded,
  route: '/admin/maintenance',
  description: 'Ventanas de mantenimiento',
);
const emergencyLockItem = AdminNavItem(
  label: 'Bloqueo de emergencia',
  icon: Icons.emergency_rounded,
  route: '/admin/emergency-lock',
  description: 'Suspende un usuario al instante',
);
const reversalItem = AdminNavItem(
  label: 'Reversión de transacción',
  icon: Icons.undo_rounded,
  route: '/admin/transaction-reversal',
  description: 'Revierte una operación puntual',
);
const resetCredentialsItem = AdminNavItem(
  label: 'Reseteo de credenciales',
  icon: Icons.lock_reset_rounded,
  route: '/admin/reset-credentials',
  description: 'Fuerza el cambio de clave de un socio',
);
const reportsItem = AdminNavItem(
  label: 'Reportes regulatorios',
  icon: Icons.assessment_rounded,
  route: '/admin/reports',
  description: 'Descarga reportes oficiales',
);
const auditLogItem = AdminNavItem(
  label: 'Auditoría',
  icon: Icons.history_rounded,
  route: '/admin/audit-log',
  description: 'Historial de acciones administrativas',
);

const adminNavSections = <AdminNavSection>[
  AdminNavSection('General', [dashboardItem]),
  AdminNavSection('Operaciones', [partnersItem, accountsItem, loansItem]),
  AdminNavSection('Riesgo y cumplimiento', [complianceItem, riskRulesItem, guaranteeFundItem, limitsItem]),
  AdminNavSection('Configuración', [feesItem, maintenanceItem]),
  AdminNavSection('Acciones críticas', [emergencyLockItem, reversalItem, resetCredentialsItem]),
  AdminNavSection('Reportes', [reportsItem, auditLogItem]),
];

/// Sección del menú a la que pertenece una ruta (para el encabezado de la página).
String? sectionTitleFor(String route) {
  for (final section in adminNavSections) {
    if (section.items.any((i) => i.route == route)) return section.title;
  }
  return null;
}
