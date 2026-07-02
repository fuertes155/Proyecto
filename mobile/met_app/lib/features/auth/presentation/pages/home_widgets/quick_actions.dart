import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class QuickActions extends StatelessWidget {
  const QuickActions({super.key, required this.onTapMore});

  final VoidCallback onTapMore;

  void _showActionSheet(BuildContext context, String title, IconData icon, List<Widget> children) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => Padding(
        padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
        child: Container(
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
              const SizedBox(height: 24),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24),
                child: Column(children: children),
              ),
              const SizedBox(height: 32),
            ],
          ),
        ),
      ),
    );
  }

  void _showTransferSheet(BuildContext context) {
    _showActionSheet(context, 'Transferir Dinero', Icons.swap_horiz, [
      _buildTextField(context, 'Número de cuenta o celular', Icons.account_balance_wallet_outlined),
      const SizedBox(height: 16),
      _buildTextField(context, 'Monto a transferir (\$)', Icons.attach_money, isNumeric: true),
      const SizedBox(height: 24),
      _buildPrimaryButton(context, 'Continuar', () => Navigator.pop(context)),
    ]);
  }

  void _showPaySheet(BuildContext context) {
    _showActionSheet(context, 'Pagar Servicios', Icons.payment, [
      _buildTextField(context, 'Referencia de pago', Icons.receipt_long_outlined),
      const SizedBox(height: 16),
      _buildTextField(context, 'Valor a pagar (\$)', Icons.attach_money, isNumeric: true),
      const SizedBox(height: 24),
      _buildPrimaryButton(context, 'Buscar Factura', () => Navigator.pop(context)),
    ]);
  }

  void _showTopUpSheet(BuildContext context) {
    _showActionSheet(context, 'Recargar Celular', Icons.add_circle_outline, [
      _buildTextField(context, 'Número de celular', Icons.phone_android),
      const SizedBox(height: 16),
      _buildTextField(context, 'Monto de recarga (\$)', Icons.attach_money, isNumeric: true),
      const SizedBox(height: 24),
      _buildPrimaryButton(context, 'Recargar ahora', () => Navigator.pop(context)),
    ]);
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
          icon: Icons.payment,
          label: 'Pagar',
          onTap: () => _showPaySheet(context),
        ),
        _QuickAction(
          icon: Icons.add_circle_outline,
          label: 'Recargar',
          onTap: () => _showTopUpSheet(context),
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
