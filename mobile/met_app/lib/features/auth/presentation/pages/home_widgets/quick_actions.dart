import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class QuickActions extends StatelessWidget {
  const QuickActions({super.key, required this.onTapMore});

  final VoidCallback onTapMore;

  void _showActionSheet(BuildContext context, String title, IconData icon, Widget content) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => Padding(
        padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
        child: Container(
          constraints: BoxConstraints(
            maxHeight: MediaQuery.of(context).size.height * 0.85,
          ),
          decoration: BoxDecoration(
            color: Theme.of(context).scaffoldBackgroundColor,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.2),
                blurRadius: 20,
                spreadRadius: 5,
              )
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const SizedBox(height: 12),
              Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.onSurface.withValues(alpha: 0.2),
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(height: 24),
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.primary.withValues(alpha: 0.15),
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: Theme.of(context).colorScheme.primary, size: 32),
              ),
              const SizedBox(height: 16),
              Text(
                title,
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: Theme.of(context).colorScheme.onSurface,
                ),
              ),
              const SizedBox(height: 16),
              Flexible(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.only(left: 24, right: 24, bottom: 32),
                  child: content,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showDepositSheet(BuildContext context) {
    _showActionSheet(context, 'Recargar tu cuenta', Icons.account_balance_wallet, Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildDepositMethodTile(
          context,
          'Nequi',
          'Normalmente en 5 minutos | Depósito más rápido',
          const Text('Nequi', style: TextStyle(color: Color(0xFFE10098), fontWeight: FontWeight.bold, fontSize: 16, letterSpacing: -0.5)),
        ),
        
        _buildSectionHeader(context, 'BANCO'),
        _buildDepositMethodTile(
          context,
          'Bre-B',
          'Instantáneo',
          const Text('Bre-B', style: TextStyle(color: Color(0xFF00C853), fontWeight: FontWeight.bold, fontSize: 16)),
        ),
        _buildDepositMethodTile(
          context,
          'Bancolombia',
          'PSE | Normalmente en 10 minutos',
          Container(
            padding: const EdgeInsets.all(4),
            decoration: BoxDecoration(color: const Color(0xFFFFD100), borderRadius: BorderRadius.circular(6)),
            child: const Text('Bc', style: TextStyle(color: Color(0xFF002855), fontWeight: FontWeight.w900, fontSize: 16)),
          ),
          method: 'PSE',
          bankHint: 'Bancolombia',
        ),
        _buildDepositMethodTile(
          context,
          'Davivienda',
          'PSE | Normalmente en 10 minutos',
          Container(
            padding: const EdgeInsets.all(4),
            decoration: BoxDecoration(color: const Color(0xFFEE3831), borderRadius: BorderRadius.circular(6)),
            child: const Text('Dv', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 16)),
          ),
          method: 'PSE',
          bankHint: 'Davivienda',
        ),
        _buildDepositMethodTile(
          context,
          'BBVA',
          'PSE | Normalmente en 10 minutos',
          Container(
            padding: const EdgeInsets.all(4),
            decoration: BoxDecoration(color: const Color(0xFF004481), borderRadius: BorderRadius.circular(6)),
            child: const Text('BBVA', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 11)),
          ),
          method: 'PSE',
          bankHint: 'BBVA',
        ),
        _buildDepositMethodTile(
          context,
          'Banco de Bogotá',
          'PSE | Normalmente en 10 minutos',
          Container(
            padding: const EdgeInsets.all(4),
            decoration: BoxDecoration(color: const Color(0xFFDA291C), borderRadius: BorderRadius.circular(6)),
            child: const Text('BB', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 16)),
          ),
          method: 'PSE',
          bankHint: 'Bogotá',
        ),
        _buildDepositMethodTile(
          context,
          'Otro banco (PSE)',
          'Elige tu banco entre más de 14 disponibles',
          Container(
            padding: const EdgeInsets.all(4),
            decoration: BoxDecoration(color: const Color(0xFF003057), borderRadius: BorderRadius.circular(20)),
            child: const Text('PSE', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 11)),
          ),
          method: 'PSE',
        ),

        _buildSectionHeader(context, 'TARJETAS'),
        _buildDepositMethodTile(
          context,
          'Tarjeta de crédito/débito',
          'Visa, Mastercard y más | Al instante',
          Container(
            padding: const EdgeInsets.all(6),
            decoration: BoxDecoration(color: const Color(0xFF1A1F71), borderRadius: BorderRadius.circular(8)),
            child: const Icon(Icons.credit_card, color: Colors.white, size: 20),
          ),
        ),

        _buildSectionHeader(context, 'WALLETS'),
        _buildDepositMethodTile(
          context,
          'Daviplata',
          'Normalmente en 10 minutos',
          const Text('DaviPlata', style: TextStyle(color: Color(0xFFED1C24), fontWeight: FontWeight.w900, fontSize: 13, fontStyle: FontStyle.italic)),
        ),

        _buildSectionHeader(context, 'EFECTIVO'),
        _buildDepositMethodTile(
          context,
          'Efecty',
          'Hasta 5 minutos',
          Container(
            padding: const EdgeInsets.all(4),
            decoration: BoxDecoration(color: const Color(0xFFFFCC00), borderRadius: BorderRadius.circular(4)),
            child: const Text('efecty', style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold, fontSize: 12)),
          ),
        ),
        _buildDepositMethodTile(
          context,
          'Puntored',
          'Hasta 5 minutos',
          const Text('puntored', style: TextStyle(color: Colors.black, fontWeight: FontWeight.w900, fontSize: 15, letterSpacing: -0.5)),
        ),
        _buildDepositMethodTile(
          context,
          'Baloto',
          'Hasta 5 minutos',
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
            decoration: BoxDecoration(color: const Color(0xFFE30613), borderRadius: BorderRadius.circular(4)),
            child: const Text('Baloto', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 13, fontStyle: FontStyle.italic)),
          ),
        ),
        _buildDepositMethodTile(
          context,
          'Gana',
          'Hasta 5 minutos',
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(color: const Color(0xFF00A651), borderRadius: BorderRadius.circular(4)),
            child: const Text('gana', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 14)),
          ),
        ),
        _buildDepositMethodTile(
          context,
          'Corresponsal Bancolombia',
          'Paga en efectivo en un punto Bancolombia',
          Container(
            padding: const EdgeInsets.all(4),
            decoration: BoxDecoration(color: const Color(0xFFFFD100), borderRadius: BorderRadius.circular(4)),
            child: const Text('BC', style: TextStyle(color: Color(0xFF003057), fontWeight: FontWeight.w900, fontSize: 14)),
          ),
        ),
      ]
    ));
  }

  Widget _buildSectionHeader(BuildContext context, String title) {
    return Padding(
      padding: const EdgeInsets.only(top: 24, bottom: 8, left: 4),
      child: Text(
        title,
        style: TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.bold,
          color: Theme.of(context).colorScheme.onSurface.withValues(alpha: 0.6),
          letterSpacing: 1.2,
        ),
      ),
    );
  }

  Widget _buildDepositMethodTile(
    BuildContext context,
    String title,
    String subtitle,
    Widget leadingLogo, {
    String? method,
    String? bankHint,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Theme.of(context).brightness == Brightness.dark 
            ? Theme.of(context).colorScheme.onSurface.withValues(alpha: 0.05)
            : Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: Theme.of(context).brightness == Brightness.dark 
              ? Theme.of(context).colorScheme.onSurface.withValues(alpha: 0.1)
              : Colors.grey.withValues(alpha: 0.2)
        ),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () {
            Navigator.pop(context);
            context.push('/deposit', extra: {
              'method': method ?? title,
              if (bankHint != null) 'bankHint': bankHint,
            });
          },
          borderRadius: BorderRadius.circular(12),
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              children: [
                Container(
                  width: 56,
                  height: 36,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: Theme.of(context).brightness == Brightness.dark
                        ? Colors.white.withValues(alpha: 0.9)
                        : Colors.transparent,
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: FittedBox(fit: BoxFit.scaleDown, child: leadingLogo),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.bold,
                          color: Theme.of(context).colorScheme.onSurface,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        subtitle,
                        style: TextStyle(
                          fontSize: 13,
                          color: Theme.of(context).colorScheme.onSurface.withValues(alpha: 0.6),
                        ),
                      ),
                    ],
                  ),
                ),
                Icon(
                  Icons.chevron_right,
                  color: Theme.of(context).colorScheme.onSurface.withValues(alpha: 0.4),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 6),
      decoration: BoxDecoration(
        color: isDark ? scheme.surfaceContainer : Colors.white,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: scheme.onSurface.withValues(alpha: isDark ? 0.08 : 0.05)),
        boxShadow: isDark
            ? null
            : [BoxShadow(color: Colors.black.withValues(alpha: 0.035), blurRadius: 12, offset: const Offset(0, 2))],
      ),
      child: Row(
        children: [
          Expanded(
            child: _QuickAction(
              icon: Icons.add_rounded,
              label: 'Recargar',
              onTap: () => _showDepositSheet(context),
            ),
          ),
          Expanded(
            child: _QuickAction(
              icon: Icons.swap_horiz_rounded,
              label: 'Transferir',
              onTap: () => context.push('/transfer'),
            ),
          ),
          Expanded(
            child: _QuickAction(
              icon: Icons.north_east_rounded,
              label: 'Retirar',
              onTap: () => context.push('/accounts/external'),
            ),
          ),
          Expanded(
            child: _QuickAction(
              icon: Icons.grid_view_rounded,
              label: 'Más',
              onTap: onTapMore,
            ),
          ),
        ],
      ),
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
    final scheme = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Semantics(
      button: true,
      label: label,
      excludeSemantics: true,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(18),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 52,
                height: 52,
                decoration: BoxDecoration(
                  color: scheme.primary.withValues(alpha: isDark ? 0.18 : 0.1),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Icon(icon, color: scheme.primary, size: 26),
              ),
              const SizedBox(height: 8),
              Text(
                label,
                style: TextStyle(
                  color: scheme.onSurface.withValues(alpha: 0.85),
                  fontSize: 13,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
