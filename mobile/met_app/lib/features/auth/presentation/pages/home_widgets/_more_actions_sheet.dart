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
