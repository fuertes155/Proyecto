import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/widgets/accessible_button.dart';
import '../../data/models/register_external_bank_account_request.dart';
import '../providers/external_accounts_provider.dart';

class AddExternalBankAccountPage extends ConsumerStatefulWidget {
  const AddExternalBankAccountPage({super.key});

  @override
  ConsumerState<AddExternalBankAccountPage> createState() => _AddExternalBankAccountPageState();
}

class _AddExternalBankAccountPageState extends ConsumerState<AddExternalBankAccountPage> {
  final _formKey = GlobalKey<FormState>();
  final _accountNumberController = TextEditingController();
  String? _bankCode;
  String _accountType = 'SAVINGS';
  bool _isLoading = false;

  @override
  void dispose() {
    _accountNumberController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate() || _bankCode == null) {
      if (_bankCode == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Selecciona un banco')),
        );
      }
      return;
    }

    setState(() => _isLoading = true);
    try {
      await ref.read(externalBankAccountsListProvider.notifier).register(
            RegisterExternalBankAccountRequest(
              bankCode: _bankCode!,
              accountType: _accountType,
              accountNumber: _accountNumberController.text.trim(),
            ),
          );
      if (!mounted) return;
      context.pop();
    } on DioException catch (e) {
      if (!mounted) return;
      String message = 'No fue posible registrar la cuenta';
      if (e.response?.data is Map && (e.response!.data as Map).containsKey('message')) {
        message = (e.response!.data as Map)['message'] as String;
      }
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No fue posible registrar la cuenta')),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final banksAsync = ref.watch(banksProvider('PAYOUT'));

    return Scaffold(
      appBar: AppBar(title: const Text('Nueva cuenta bancaria')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  'La cuenta debe estar a tu propio nombre — solo puedes retirar a cuentas bancarias tuyas.',
                  style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant),
                ),
                const SizedBox(height: 24),
                banksAsync.when(
                  loading: () => const Padding(
                    padding: EdgeInsets.symmetric(vertical: 16),
                    child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
                  ),
                  error: (error, _) => Text(
                    'No fue posible cargar la lista de bancos',
                    style: TextStyle(color: Theme.of(context).colorScheme.error),
                  ),
                  data: (banks) => DropdownButtonFormField<String>(
                    value: _bankCode,
                    decoration: const InputDecoration(labelText: 'Banco'),
                    items: banks
                        .map((bank) => DropdownMenuItem(value: bank.code, child: Text(bank.name)))
                        .toList(),
                    onChanged: (v) => setState(() => _bankCode = v),
                  ),
                ),
                const SizedBox(height: 16),
                DropdownButtonFormField<String>(
                  value: _accountType,
                  decoration: const InputDecoration(labelText: 'Tipo de cuenta'),
                  items: const [
                    DropdownMenuItem(value: 'SAVINGS', child: Text('Ahorros')),
                    DropdownMenuItem(value: 'CHECKING', child: Text('Corriente')),
                  ],
                  onChanged: (v) => setState(() => _accountType = v ?? 'SAVINGS'),
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _accountNumberController,
                  decoration: const InputDecoration(labelText: 'Número de cuenta'),
                  keyboardType: TextInputType.number,
                  inputFormatters: [
                    FilteringTextInputFormatter.digitsOnly,
                    LengthLimitingTextInputFormatter(20),
                  ],
                  validator: (v) {
                    if (v == null || v.length < 6) return 'Debe tener entre 6 y 20 dígitos';
                    return null;
                  },
                ),
                const SizedBox(height: 32),
                AccessibleButton(
                  label: 'Registrar cuenta',
                  isLoading: _isLoading,
                  onPressed: _submit,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
