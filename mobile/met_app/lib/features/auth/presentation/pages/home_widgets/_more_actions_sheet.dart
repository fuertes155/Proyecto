import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:met/features/auth/presentation/pages/home_widgets/_sheet_action_tile.dart';


class MoreActionsSheet {
  static void show(BuildContext context) {
    // Se cierra la hoja antes de navegar: si no, al volver de la pantalla sigue abierta.
    void go(BuildContext sheetContext, String route) {
      Navigator.of(sheetContext).pop();
      context.push(route);
    }

    showModalBottomSheet(
      context: context,
      backgroundColor: Theme.of(context).colorScheme.surfaceContainer,
      showDragHandle: true,
      builder: (sheetContext) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding: const EdgeInsets.only(left: 4, bottom: 14),
                  child: Text(
                    'Más opciones',
                    style: Theme.of(sheetContext).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
                  ),
                ),
                Wrap(
                  spacing: 12,
                  runSpacing: 12,
                  children: [
                    SheetActionTile(
                      icon: Icons.receipt_long_outlined,
                      title: 'Mis movimientos',
                      subtitle: 'Extracto de transacciones',
                      onTap: () => go(sheetContext, '/statement'),
                    ),
                    SheetActionTile(
                      icon: Icons.request_quote_outlined,
                      title: 'Mis créditos',
                      subtitle: 'Solicitudes y estado',
                      onTap: () => go(sheetContext, '/loans/applications'),
                    ),
                    SheetActionTile(
                      icon: Icons.trending_up_rounded,
                      title: 'Inversiones',
                      subtitle: 'Tu portafolio',
                      onTap: () => go(sheetContext, '/investments'),
                    ),
                    SheetActionTile(
                      icon: Icons.account_balance_outlined,
                      title: 'Cuentas bancarias',
                      subtitle: 'Para tus retiros',
                      onTap: () => go(sheetContext, '/accounts/external'),
                    ),
                    SheetActionTile(
                      icon: Icons.summarize_outlined,
                      title: 'Reportes',
                      subtitle: 'Descarga tus reportes',
                      onTap: () => go(sheetContext, '/reports'),
                    ),
                    SheetActionTile(
                      icon: Icons.support_agent_rounded,
                      title: 'Soporte 24/7',
                      subtitle: 'Chatea con nosotros',
                      onTap: () => go(sheetContext, '/support/chat'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
