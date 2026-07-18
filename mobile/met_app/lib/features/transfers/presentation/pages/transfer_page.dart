import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../providers/transfer_ui_provider.dart';
import '../providers/transfers_provider.dart';
import 'package:flutter_animate/flutter_animate.dart';

class TransferPage extends ConsumerStatefulWidget {
  const TransferPage({super.key});

  @override
  ConsumerState<TransferPage> createState() => _TransferPageState();
}

class _TransferPageState extends ConsumerState<TransferPage> {
  final _identifierController = TextEditingController();
  final _amountController = TextEditingController();
  final _conceptController = TextEditingController();
  final _pinController = TextEditingController();
  final _otpController = TextEditingController();

  @override
  void dispose() {
    _identifierController.dispose();
    _amountController.dispose();
    _conceptController.dispose();
    _pinController.dispose();
    _otpController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(transferNotifierProvider);
    final notifier = ref.read(transferNotifierProvider.notifier);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Transferir Dinero'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            if (state.step > 0 && state.step < 4) {
              notifier.previousStep();
            } else {
              context.pop();
            }
          },
        ),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Progress Indicator
              Row(
                children: List.generate(5, (index) {
                  return Expanded(
                    child: Container(
                      margin: const EdgeInsets.symmetric(horizontal: 4),
                      height: 4,
                      decoration: BoxDecoration(
                        color: index <= state.step 
                            ? Theme.of(context).colorScheme.primary 
                            : Theme.of(context).colorScheme.surfaceContainerHighest,
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  );
                }),
              ),
              const SizedBox(height: 32),
              
              if (state.error != null)
                Container(
                  padding: const EdgeInsets.all(12),
                  margin: const EdgeInsets.only(bottom: 24),
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.errorContainer,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Row(
                    children: [
                      Icon(Icons.error_outline, color: Theme.of(context).colorScheme.error),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          state.error!,
                          style: TextStyle(color: Theme.of(context).colorScheme.onErrorContainer),
                        ),
                      ),
                    ],
                  ),
                ).animate().fade(),

              Expanded(
                child: AnimatedSwitcher(
                  duration: const Duration(milliseconds: 300),
                  child: _buildStep(state, notifier),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStep(TransferState state, TransferNotifier notifier) {
    switch (state.step) {
      case 0:
        return _buildRecipientStep(state, notifier);
      case 1:
        return _buildAmountStep(state, notifier);
      case 2:
        return _buildPinStep(state, notifier);
      case 3:
        return _buildOtpStep(state, notifier);
      case 4:
        return _buildSuccessStep(state);
      default:
        return const SizedBox();
    }
  }

  // Destinatarios recientes (mock visual — se puede conectar al historial real)
  static const List<Map<String, String>> _recentRecipients = [
    {'name': 'María García', 'id': '3001234567', 'initial': 'M'},
    {'name': 'Carlos López', 'id': '3109876543', 'initial': 'C'},
    {'name': 'Ana Martínez', 'id': '3205551234', 'initial': 'A'},
  ];

  static const List<Color> _avatarColors = [
    Color(0xFF2E7D32),
    Color(0xFF1565C0),
    Color(0xFF6A1B9A),
  ];

  Widget _buildRecipientStep(TransferState state, TransferNotifier notifier) {
    final primary = Theme.of(context).colorScheme.primary;

    return Column(
      key: const ValueKey('step_0'),
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Contenido desplazable
        Expanded(
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '¿A quién le vas a enviar?',
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 8),
                Text(
                  'Ingresa el número de cuenta o celular del destinatario.',
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 28),

                // Campo de búsqueda
                TextField(
                  controller: _identifierController,
                  keyboardType: TextInputType.number,
                  decoration: InputDecoration(
                    labelText: 'Número de cuenta o celular',
                    prefixIcon: const Icon(Icons.person_search_outlined),
                    suffixIcon: _identifierController.text.isNotEmpty
                        ? IconButton(
                            icon: const Icon(Icons.clear, size: 18),
                            onPressed: () {
                              _identifierController.clear();
                              notifier.updateIdentifier('');
                              setState(() {});
                            },
                          )
                        : null,
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(14)),
                    filled: true,
                    fillColor: Theme.of(context).colorScheme.surface,
                  ),
                  onChanged: (val) {
                    notifier.updateIdentifier(val);
                    setState(() {});
                  },
                ),

                const SizedBox(height: 28),

                // Sección de recientes (solo si el campo está vacío)
                if (state.recipientIdentifier.isEmpty) ...[
                  Row(
                    children: [
                      const Icon(Icons.history, size: 16, color: Colors.grey),
                      const SizedBox(width: 6),
                      Text(
                        'Recientes',
                        style: Theme.of(context).textTheme.labelLarge?.copyWith(
                          color: Theme.of(context).colorScheme.onSurfaceVariant,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  ..._recentRecipients.asMap().entries.map((entry) {
                    final i = entry.key;
                    final r = entry.value;
                    return Container(
                      margin: const EdgeInsets.only(bottom: 10),
                      decoration: BoxDecoration(
                        color: Theme.of(context).colorScheme.surface,
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(
                          color: Theme.of(context).colorScheme.outlineVariant,
                        ),
                      ),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: _avatarColors[i % _avatarColors.length],
                          child: Text(
                            r['initial']!,
                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                          ),
                        ),
                        title: Text(r['name']!, style: const TextStyle(fontWeight: FontWeight.w600)),
                        subtitle: Text(r['id']!, style: const TextStyle(fontSize: 12)),
                        trailing: const Icon(Icons.chevron_right_rounded),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                        onTap: () {
                          _identifierController.text = r['id']!;
                          notifier.updateIdentifier(r['id']!);
                          setState(() {});
                        },
                      ),
                    );
                  }),
                ],
              ],
            ),
          ),
        ),

        // Botón fijo en la parte inferior
        const SizedBox(height: 16),
        SizedBox(
          width: double.infinity,
          height: 52,
          child: FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: primary,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            ),
            onPressed: state.isLoading
                ? null
                : () {
                    if (state.recipientIdentifier.trim().isEmpty) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Ingresa un número de cuenta o celular')),
                      );
                      return;
                    }
                    notifier.verifyRecipient();
                  },
            child: state.isLoading
                ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Text(
                    'Buscar Destinatario',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
          ),
        ),
      ],
    );
  }


  Widget _buildAmountStep(TransferState state, TransferNotifier notifier) {
    final accountOpt = ref.watch(myAccountProvider).value;
    final maxAmount = accountOpt?.interestBalance ?? 0.0;

    return Column(
      key: const ValueKey('step_1'),
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.primaryContainer.withOpacity(0.5),
            borderRadius: BorderRadius.circular(16),
          ),
          child: Row(
            children: [
              CircleAvatar(
                backgroundColor: Theme.of(context).colorScheme.primary,
                child: Text(
                  state.recipient!.ownerName.substring(0, 1).toUpperCase(),
                  style: TextStyle(color: Theme.of(context).colorScheme.onPrimary),
                ),
              ),
              const SizedBox(width: 16),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Enviando a:', style: TextStyle(fontSize: 12)),
                  Text(
                    state.recipient!.ownerName,
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 32),
        TextField(
          controller: _amountController,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          decoration: const InputDecoration(
            labelText: 'Monto a transferir (\$)',
            prefixIcon: Icon(Icons.attach_money),
          ),
          onChanged: (val) => notifier.updateAmount(double.tryParse(val) ?? 0),
        ),
        if (state.amount > maxAmount)
          Padding(
            padding: const EdgeInsets.only(top: 8.0),
            child: Text(
              'El monto supera tus intereses disponibles (\$${maxAmount.toStringAsFixed(2)})',
              style: TextStyle(color: Theme.of(context).colorScheme.error, fontSize: 12),
            ),
          ),
        const SizedBox(height: 16),
        TextField(
          controller: _conceptController,
          decoration: const InputDecoration(
            labelText: 'Concepto o mensaje (Opcional)',
            prefixIcon: Icon(Icons.edit_note),
          ),
          onChanged: notifier.updateConcept,
        ),
        const Spacer(),
        SizedBox(
          width: double.infinity,
          child: FilledButton(
            onPressed: (state.amount <= 0 || state.amount > maxAmount) ? null : () => notifier.nextStep(),
            child: const Text('Continuar'),
          ),
        ),
      ],
    );
  }

  Widget _buildPinStep(TransferState state, TransferNotifier notifier) {
    return Column(
      key: const ValueKey('step_2'),
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Confirma la transacción',
          style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 24),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.surfaceContainerHighest,
            borderRadius: BorderRadius.circular(16),
          ),
          child: Column(
            children: [
              const Text('Vas a transferir'),
              const SizedBox(height: 8),
              Text(
                '\$${state.amount.toStringAsFixed(2)}',
                style: const TextStyle(fontSize: 36, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text('a ${state.recipient!.ownerName}'),
            ],
          ),
        ),
        const SizedBox(height: 32),
        TextField(
          controller: _pinController,
          obscureText: true,
          keyboardType: TextInputType.number,
          maxLength: 4,
          textAlign: TextAlign.center,
          style: const TextStyle(fontSize: 24, letterSpacing: 16),
          decoration: const InputDecoration(
            hintText: 'PIN de 4 dígitos',
            counterText: '',
          ),
        ),
        const Spacer(),
        SizedBox(
          width: double.infinity,
          child: FilledButton(
            onPressed: state.isLoading 
                ? null 
                : () async {
                    bool success = await notifier.executeTransfer(_pinController.text);
                    if (!success && notifier.state.error == 'Se requiere código de seguridad para transferir') {
                       await notifier.requestOtp();
                       notifier.nextStep(); // Mover a step 3 (OTP)
                    }
                },
            child: state.isLoading 
                ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2))
                : const Text('Confirmar y Enviar'),
          ),
        ),
      ],
    );
  }

  Widget _buildOtpStep(TransferState state, TransferNotifier notifier) {
    return Column(
      key: const ValueKey('step_3'),
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Código de Seguridad (OTP)',
          style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        Text(
          'Hemos enviado un código de 6 dígitos a tu correo electrónico para confirmar esta transferencia.',
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
            color: Theme.of(context).colorScheme.onSurfaceVariant
          ),
        ),
        const SizedBox(height: 32),
        TextField(
          controller: _otpController,
          keyboardType: TextInputType.number,
          maxLength: 6,
          textAlign: TextAlign.center,
          style: const TextStyle(fontSize: 24, letterSpacing: 8),
          decoration: const InputDecoration(
            hintText: '000000',
            counterText: '',
          ),
          onChanged: notifier.updateOtp,
        ),
        const SizedBox(height: 16),
        Center(
          child: TextButton(
            onPressed: () => notifier.requestOtp(),
            child: const Text('Reenviar código'),
          ),
        ),
        const Spacer(),
        SizedBox(
          width: double.infinity,
          child: FilledButton(
            onPressed: state.isLoading || state.otp.length < 6
                ? null 
                : () => notifier.executeTransfer(_pinController.text), // Uses state.otp automatically
            child: state.isLoading 
                ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2))
                : const Text('Validar y Transferir'),
          ),
        ),
      ],
    );
  }

  Widget _buildSuccessStep(TransferState state) {
    return Center(
      key: const ValueKey('step_4'),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.check_circle, color: Colors.green, size: 100)
              .animate()
              .scale(duration: 500.ms, curve: Curves.elasticOut),
          const SizedBox(height: 24),
          Text(
            '¡Transferencia Exitosa!',
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
          ).animate().fade(delay: 300.ms),
          const SizedBox(height: 8),
          Text('Se han enviado \$${state.amount.toStringAsFixed(2)} a ${state.recipient!.ownerName}')
              .animate().fade(delay: 400.ms),
          const SizedBox(height: 48),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: () => context.go('/'),
              child: const Text('Volver al Inicio'),
            ),
          ).animate().fade(delay: 500.ms),
        ],
      ),
    );
  }
}
