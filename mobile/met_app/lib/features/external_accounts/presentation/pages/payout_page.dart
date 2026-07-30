import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../transfers/presentation/providers/transfers_provider.dart';
import '../../data/models/external_bank_account_model.dart';
import '../providers/external_accounts_provider.dart';
import '../providers/payout_ui_provider.dart';

class PayoutPage extends ConsumerStatefulWidget {
  const PayoutPage({super.key, this.preselectedAccountId});

  final String? preselectedAccountId;

  @override
  ConsumerState<PayoutPage> createState() => _PayoutPageState();
}

class _PayoutPageState extends ConsumerState<PayoutPage> {
  final _amountController = TextEditingController();
  final _conceptController = TextEditingController();
  final _pinController = TextEditingController();
  final _otpController = TextEditingController();
  bool _autoSelectAttempted = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(externalBankAccountsListProvider.notifier).load());
  }

  @override
  void dispose() {
    _amountController.dispose();
    _conceptController.dispose();
    _pinController.dispose();
    _otpController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(payoutNotifierProvider);
    final notifier = ref.read(payoutNotifierProvider.notifier);

    // Si venimos con una cuenta preseleccionada (desde la lista de cuentas),
    // saltamos directo al paso de monto la primera vez que la lista carga.
    if (widget.preselectedAccountId != null && !_autoSelectAttempted && state.selectedAccount == null) {
      final accountsAsync = ref.watch(externalBankAccountsListProvider);
      accountsAsync.whenData((accounts) {
        final match = accounts.where((a) => a.id == widget.preselectedAccountId).toList();
        if (match.isNotEmpty) {
          _autoSelectAttempted = true;
          Future.microtask(() => notifier.selectAccount(match.first));
        }
      });
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Retirar a mi banco'),
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

  Widget _buildStep(PayoutState state, PayoutNotifier notifier) {
    switch (state.step) {
      case 0:
        return _buildAccountStep(notifier);
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

  Widget _buildAccountStep(PayoutNotifier notifier) {
    final accountsAsync = ref.watch(externalBankAccountsListProvider);

    return Column(
      key: const ValueKey('step_0'),
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '¿A cuál de tus cuentas retiras?',
          style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 24),
        Expanded(
          child: accountsAsync.when(
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (error, _) => Center(child: Text('Error: $error')),
            data: (accounts) {
              final usable = accounts.where((a) => a.isUsable).toList();
              if (usable.isEmpty) {
                return Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Text('No tienes cuentas bancarias disponibles', textAlign: TextAlign.center),
                      const SizedBox(height: 16),
                      OutlinedButton(
                        onPressed: () async {
                          await context.push('/accounts/external/add');
                          ref.read(externalBankAccountsListProvider.notifier).load();
                        },
                        child: const Text('Registrar una cuenta'),
                      ),
                    ],
                  ),
                );
              }
              return ListView.separated(
                itemCount: usable.length,
                separatorBuilder: (_, __) => const SizedBox(height: 10),
                itemBuilder: (context, index) {
                  final account = usable[index];
                  return Container(
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.surface,
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: Theme.of(context).colorScheme.outlineVariant),
                    ),
                    child: ListTile(
                      leading: const Icon(Icons.account_balance_outlined),
                      title: Text(account.bankName, style: const TextStyle(fontWeight: FontWeight.w600)),
                      subtitle: Text('${account.accountTypeLabel} · ${account.maskedAccountNumber}'),
                      trailing: const Icon(Icons.chevron_right_rounded),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                      onTap: () => notifier.selectAccount(account),
                    ),
                  );
                },
              );
            },
          ),
        ),
      ],
    );
  }

  Widget _buildAmountStep(PayoutState state, PayoutNotifier notifier) {
    final accountOpt = ref.watch(myAccountProvider).value;
    // Solo se puede retirar de las ganancias (interestBalance) — el capital
    // (principalBalance) permanece invertido en la plataforma.
    final maxAmount = accountOpt?.interestBalance ?? 0.0;
    final ExternalBankAccountModel account = state.selectedAccount!;

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
                child: const Icon(Icons.account_balance_outlined, color: Colors.white),
              ),
              const SizedBox(width: 16),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Retirando a:', style: TextStyle(fontSize: 12)),
                  Text(
                    '${account.bankName} · ${account.maskedAccountNumber}',
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Solo puedes retirar tus ganancias — el capital permanece invertido.',
          style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant, fontSize: 12),
        ),
        const SizedBox(height: 24),
        TextField(
          controller: _amountController,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          decoration: InputDecoration(
            labelText: 'Monto a retirar (\$)',
            prefixIcon: const Icon(Icons.attach_money),
            helperText: 'Ganancias disponibles: \$${maxAmount.toStringAsFixed(2)}',
          ),
          onChanged: (val) => notifier.updateAmount(double.tryParse(val) ?? 0),
        ),
        if (state.amount > maxAmount)
          Padding(
            padding: const EdgeInsets.only(top: 8.0),
            child: Text(
              'El monto supera tus ganancias disponibles (\$${maxAmount.toStringAsFixed(2)})',
              style: TextStyle(color: Theme.of(context).colorScheme.error, fontSize: 12),
            ),
          ),
        const SizedBox(height: 16),
        TextField(
          controller: _conceptController,
          decoration: const InputDecoration(
            labelText: 'Concepto (Opcional)',
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

  Widget _buildPinStep(PayoutState state, PayoutNotifier notifier) {
    return Column(
      key: const ValueKey('step_2'),
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Confirma el retiro',
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
              const Text('Vas a retirar'),
              const SizedBox(height: 8),
              Text(
                '\$${state.amount.toStringAsFixed(2)}',
                style: const TextStyle(fontSize: 36, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text('a ${state.selectedAccount!.bankName}'),
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
                    bool success = await notifier.executePayout(_pinController.text);
                    final currentError = ref.read(payoutNotifierProvider).error;
                    if (!success && currentError == 'Se requiere código de seguridad para retirar') {
                      await notifier.requestOtp();
                      notifier.nextStep();
                    }
                  },
            child: state.isLoading
                ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2))
                : const Text('Confirmar y Retirar'),
          ),
        ),
      ],
    );
  }

  Widget _buildOtpStep(PayoutState state, PayoutNotifier notifier) {
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
          'Hemos enviado un código de 6 dígitos a tu correo electrónico para confirmar este retiro.',
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
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
                : () => notifier.executePayout(_pinController.text),
            child: state.isLoading
                ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2))
                : const Text('Validar y Retirar'),
          ),
        ),
      ],
    );
  }

  Widget _buildSuccessStep(PayoutState state) {
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
            '¡Retiro Iniciado!',
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
          ).animate().fade(delay: 300.ms),
          const SizedBox(height: 8),
          Text(
            'Tu retiro de \$${state.amount.toStringAsFixed(2)} está en proceso hacia ${state.selectedAccount!.bankName}. '
            'Te avisaremos cuando se confirme.',
            textAlign: TextAlign.center,
          ).animate().fade(delay: 400.ms),
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
