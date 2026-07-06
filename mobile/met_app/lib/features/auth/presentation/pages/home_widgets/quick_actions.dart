import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:flutter/services.dart';

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
                color: Colors.black.withOpacity(0.2),
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
                  color: Theme.of(context).colorScheme.onSurface.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(height: 24),
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.primary.withOpacity(0.15),
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

  void _showTransferSheet(BuildContext context) {
    context.push('/transfer');
  }

  void _showPaySheet(BuildContext context) {
    _showActionSheet(context, 'Pagar Servicios', Icons.payment, Column(
      children: [
        _buildTextField(context, 'Referencia de pago', Icons.receipt_long_outlined),
        const SizedBox(height: 16),
        _buildTextField(context, 'Valor a pagar (\$)', Icons.attach_money, isNumeric: true),
        const SizedBox(height: 24),
        _buildPrimaryButton(context, 'Buscar Factura', () => Navigator.pop(context)),
      ]
    ));
  }

  void _showDepositSheet(BuildContext context) {
    _showActionSheet(context, 'Depósito', Icons.account_balance_wallet, Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildDepositMethodTile(
          context,
          'Nequi',
          'Normalmente en 5 minutos | Depósito más rápido',
          Text('Nequi', style: TextStyle(color: Color(0xFFE10098), fontWeight: FontWeight.bold, fontSize: 16, letterSpacing: -0.5)),
        ),
        
        _buildSectionHeader(context, 'BANCO Y TARJETAS'),
        _buildDepositMethodTile(
          context,
          'Bre-B',
          'Instantáneo',
          Text('Bre-B', style: TextStyle(color: Color(0xFF00C853), fontWeight: FontWeight.bold, fontSize: 16)),
        ),
        _buildDepositMethodTile(
          context,
          'PSE',
          'Normalmente en 10 minutos',
          Container(
            padding: EdgeInsets.all(4),
            decoration: BoxDecoration(color: Color(0xFF003057), borderRadius: BorderRadius.circular(20)),
            child: Text('PSE', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 11)),
          ),
        ),
        _buildDepositMethodTile(
          context,
          'Tarjeta de Crédito/Débito',
          'Instantáneo',
          Icon(Icons.credit_card, color: Colors.deepOrange, size: 28),
        ),
        
        _buildSectionHeader(context, 'WALLETS'),
        _buildDepositMethodTile(
          context,
          'Daviplata',
          'Normalmente en 10 minutos',
          Text('DaviPlata', style: TextStyle(color: Color(0xFFED1C24), fontWeight: FontWeight.w900, fontSize: 13, fontStyle: FontStyle.italic)),
        ),

        _buildSectionHeader(context, 'EFECTIVO'),
        _buildDepositMethodTile(
          context,
          'Efecty',
          'Hasta 5 minutos',
          Container(
            padding: EdgeInsets.all(4),
            decoration: BoxDecoration(color: Color(0xFFFFCC00), borderRadius: BorderRadius.circular(4)),
            child: Text('efecty', style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold, fontSize: 12)),
          ),
        ),
        _buildDepositMethodTile(
          context,
          'Puntored',
          'Hasta 5 minutos',
          Text('puntored', style: TextStyle(color: Colors.black, fontWeight: FontWeight.w900, fontSize: 15, letterSpacing: -0.5)),
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
          color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6),
          letterSpacing: 1.2,
        ),
      ),
    );
  }

  Widget _buildDepositMethodTile(BuildContext context, String title, String subtitle, Widget leadingLogo) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Theme.of(context).brightness == Brightness.dark 
            ? Theme.of(context).colorScheme.onSurface.withOpacity(0.05)
            : Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: Theme.of(context).brightness == Brightness.dark 
              ? Theme.of(context).colorScheme.onSurface.withOpacity(0.1)
              : Colors.grey.withOpacity(0.2)
        ),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () => Navigator.pop(context),
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
                        ? Colors.white.withOpacity(0.9)
                        : Colors.transparent,
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: leadingLogo,
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
                          color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6),
                        ),
                      ),
                    ],
                  ),
                ),
                Icon(
                  Icons.chevron_right,
                  color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildTextField(BuildContext context, String label, IconData icon, {bool isNumeric = false}) {
    return TextField(
      keyboardType: isNumeric ? TextInputType.number : TextInputType.text,
      inputFormatters: isNumeric ? [FilteringTextInputFormatter.digitsOnly] : null,
      style: TextStyle(color: Theme.of(context).colorScheme.onSurface),
      decoration: InputDecoration(
        labelText: label,
        labelStyle: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6)),
        prefixIcon: Icon(icon, color: Theme.of(context).colorScheme.primary),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.1)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: Theme.of(context).colorScheme.primary, width: 2),
        ),
        filled: true,
        fillColor: Theme.of(context).colorScheme.onSurface.withOpacity(0.02),
      ),
    );
  }

  Widget _buildPrimaryButton(BuildContext context, String text, VoidCallback onPressed) {
    return SizedBox(
      width: double.infinity,
      height: 54,
      child: ElevatedButton(
        onPressed: onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: Theme.of(context).colorScheme.primary,
          foregroundColor: Theme.of(context).colorScheme.onPrimary,
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
        ),
        child: Text(
          text,
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        _QuickAction(
          icon: Icons.swap_horiz,
          label: 'Transferir',
          onTap: () => _showTransferSheet(context),
        ),
        _QuickAction(
          icon: Icons.account_balance_wallet,
          label: 'Depósito',
          onTap: () => _showDepositSheet(context),
        ),
        _QuickAction(
          icon: Icons.grid_view,
          label: 'Más',
          onTap: onTapMore,
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
