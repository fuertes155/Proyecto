import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

/// Tono semántico de un estado: define colores de fondo y texto.
enum Tone { success, warning, danger, info, neutral }

extension ToneColors on Tone {
  Color get foreground => switch (this) {
        Tone.success => MetColors.success,
        Tone.warning => MetColors.warning,
        Tone.danger => MetColors.danger,
        Tone.info => MetColors.info,
        Tone.neutral => MetColors.neutral,
      };

  Color get background => switch (this) {
        Tone.success => MetColors.successBg,
        Tone.warning => MetColors.warningBg,
        Tone.danger => MetColors.dangerBg,
        Tone.info => MetColors.infoBg,
        Tone.neutral => MetColors.neutralBg,
      };

  IconData get icon => switch (this) {
        Tone.success => Icons.check_circle_rounded,
        Tone.warning => Icons.schedule_rounded,
        Tone.danger => Icons.error_rounded,
        Tone.info => Icons.info_rounded,
        Tone.neutral => Icons.remove_circle_outline_rounded,
      };
}

/// Etiqueta legible + tono para un código de estado del backend.
class StatusInfo {
  const StatusInfo(this.label, this.tone);

  final String label;
  final Tone tone;
}

StatusInfo kycStatus(String code) => switch (code) {
      'APPROVED' => const StatusInfo('Aprobado', Tone.success),
      'REJECTED' => const StatusInfo('Rechazado', Tone.danger),
      'IN_REVIEW' => const StatusInfo('En revisión', Tone.info),
      'PENDING' || '' => const StatusInfo('Pendiente', Tone.warning),
      _ => StatusInfo(code, Tone.neutral),
    };

/// KYC que todavía requiere decisión de un administrador.
bool isKycPending(String code) => code != 'APPROVED' && code != 'REJECTED';

StatusInfo accountStatus(String code) => switch (code) {
      'ACTIVE' => const StatusInfo('Activa', Tone.success),
      'BLOCKED' => const StatusInfo('Bloqueada', Tone.danger),
      'CLOSED' => const StatusInfo('Cerrada', Tone.neutral),
      _ => StatusInfo(code, Tone.neutral),
    };

StatusInfo loanStatus(String code) => switch (code) {
      'SUBMITTED' => const StatusInfo('Nueva', Tone.warning),
      'PENDING' => const StatusInfo('Pendiente', Tone.warning),
      'IN_REVIEW' => const StatusInfo('En revisión', Tone.info),
      'APPROVED' => const StatusInfo('Aprobada', Tone.success),
      'DISBURSED' => const StatusInfo('Desembolsada', Tone.success),
      'REJECTED' => const StatusInfo('Rechazada', Tone.danger),
      'PAID_OFF' => const StatusInfo('Pagada', Tone.neutral),
      _ => StatusInfo(code, Tone.neutral),
    };

/// Solicitudes de crédito que esperan aprobación o rechazo.
bool isLoanAwaitingDecision(String code) =>
    code == 'SUBMITTED' || code == 'IN_REVIEW' || code == 'PENDING';

StatusInfo alertStatus(String code) => switch (code) {
      'OPEN' => const StatusInfo('Abierta', Tone.warning),
      'UNDER_REVIEW' => const StatusInfo('En revisión', Tone.info),
      'DISMISSED' => const StatusInfo('Descartada', Tone.neutral),
      'REPORTED' => const StatusInfo('Reportada (ROS)', Tone.danger),
      _ => StatusInfo(code, Tone.neutral),
    };

StatusInfo alertSeverity(String code) => switch (code) {
      'HIGH' => const StatusInfo('Severidad alta', Tone.danger),
      'MEDIUM' => const StatusInfo('Severidad media', Tone.warning),
      'LOW' => const StatusInfo('Severidad baja', Tone.info),
      _ => StatusInfo(code, Tone.neutral),
    };

/// Píldora de estado (p. ej. "Aprobado" en verde, "Pendiente" en ámbar).
class StatusBadge extends StatelessWidget {
  const StatusBadge(this.info, {super.key, this.showDot = true});

  StatusBadge.of(String label, Tone tone, {Key? key, bool showDot = true})
      : this(StatusInfo(label, tone), key: key, showDot: showDot);

  final StatusInfo info;
  final bool showDot;

  @override
  Widget build(BuildContext context) {
    final tone = info.tone;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: tone.background,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (showDot) ...[
            Container(
              width: 7,
              height: 7,
              decoration: BoxDecoration(color: tone.foreground, shape: BoxShape.circle),
            ),
            const SizedBox(width: 6),
          ],
          Flexible(
            child: Text(
              info.label,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(color: tone.foreground, fontSize: 12.5, fontWeight: FontWeight.w700),
            ),
          ),
        ],
      ),
    );
  }
}
