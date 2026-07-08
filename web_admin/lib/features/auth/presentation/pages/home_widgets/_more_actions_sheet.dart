import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:met/features/auth/presentation/pages/home_widgets/_sheet_action_tile.dart';


class MoreActionsSheet {
  static void show(BuildContext context) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Theme.of(context).colorScheme.surfaceContainer,
      showDragHandle: true,
      builder: (context) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                SheetActionTile(
                  icon: Icons.savings_outlined,
                  title: 'Ahorro',
                  subtitle: 'Programado',
                  onTap: () => context.push('/savings/scheduled'),
                ),
                SheetActionTile(
                  icon: Icons.handshake_outlined,
                  title: 'Solidaridad',
                  subtitle: 'Grupos',
                  onTap: () => context.push('/solidarity'),
                ),
                SheetActionTile(
                  icon: Icons.calculate_outlined,
                  title: 'Préstamos',
                  subtitle: 'Simulación',
                  onTap: () => context.push('/loans/simulate'),
                ),
                SheetActionTile(
                  icon: Icons.description_outlined,
                  title: 'Reportes',
                  subtitle: 'Regulatorios',
                  onTap: () => context.push('/compliance/reports'),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
