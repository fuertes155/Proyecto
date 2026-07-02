import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';


class QuickActions extends StatelessWidget {
  const QuickActions({super.key, required this.onTapMore});

  final VoidCallback onTapMore;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        _QuickAction(
          icon: Icons.swap_horiz,
          label: 'Transferir',
          onTap: () => context.push('/loans/applications'),
        ),
        _QuickAction(
          icon: Icons.payment,
          label: 'Pagar',
          onTap: () => context.push('/compliance/reports'),
        ),
        _QuickAction(
          icon: Icons.add_circle_outline,
          label: 'Recargar',
          onTap: () => context.push('/savings/scheduled/create'),
        ),
        _QuickAction(
          icon: Icons.grid_view,
          label: 'Más',
          onTap: () => onTapMore(),
        ),
      ],
    );
  }
}

class _QuickAction extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback? onTap;

  const _QuickAction({
    required this.icon,
    required this.label,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(24),
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.05),
              border: Border.all(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.1)),
            ),
            child: Icon(icon, color: Theme.of(context).colorScheme.onSurface, size: 24),
          ),
          const SizedBox(height: 8),
          Text(
            label,
            style:
                TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.8), fontSize: 13),
          ),
        ],
      ),
    );
  }
}
