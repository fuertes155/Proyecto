import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../../../external_accounts/data/models/bank_model.dart';
import '../../../external_accounts/presentation/providers/external_accounts_provider.dart';
import '../providers/deposit_provider.dart';

class DepositPage extends ConsumerStatefulWidget {
  final String method;

  /// Nombre (no código) de un banco a preseleccionar cuando [method] es
  /// 'PSE' — permite que un tile de un banco puntual en la pantalla de
  /// métodos salte directo al monto con el banco ya elegido, en vez de
  /// obligar a pasar por el selector genérico. Se resuelve contra el
  /// catálogo real (GET /v1/banks?type=PSE) por coincidencia de nombre.
  final String? bankHint;

  const DepositPage({super.key, required this.method, this.bankHint});

  @override
  ConsumerState<DepositPage> createState() => _DepositPageState();
}

class _DepositPageState extends ConsumerState<DepositPage> {
  final _amountController = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      ref.read(depositProvider.notifier).reset();
      ref.read(depositProvider.notifier).setMethod(widget.method);

      final hint = widget.bankHint;
      if (widget.method == 'PSE' && hint != null && hint.isNotEmpty) {
        try {
          final banks = await ref.read(banksProvider('PSE').future);
          final normalizedHint = hint.toLowerCase();
          for (final bank in banks) {
            if (bank.name.toLowerCase().contains(normalizedHint)) {
              if (mounted) {
                ref.read(depositProvider.notifier).setBankCode(bank.code);
              }
              break;
            }
          }
        } catch (_) {
          // Si falla la carga del catálogo, el usuario igual puede elegir
          // el banco manualmente desde el selector — no bloqueamos la pantalla.
        }
      }
    });

    _amountController.addListener(() {
      // Remove any non-digit characters for parsing
      final cleanText = _amountController.text.replaceAll(RegExp(r'[^0-9]'), '');
      final value = double.tryParse(cleanText) ?? 0.0;
      ref.read(depositProvider.notifier).setAmount(value);
    });
  }

  @override
  void dispose() {
    _amountController.dispose();
    super.dispose();
  }

  void _setAmount(String amount) {
    _amountController.text = amount;
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(depositProvider);

    ref.listen(depositProvider, (previous, next) async {
      if (next.paymentUrl != null && next.paymentUrl != previous?.paymentUrl) {
        // Navegar al WebView embebido en lugar de abrir el navegador externo
        if (mounted) {
          context.push('/deposit/webview', extra: {
            'paymentUrl': next.paymentUrl!,
            'method': widget.method,
            'amount': next.amount,
          });
        }
      } else if (next.error != null && next.error != previous?.error) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.error!)),
        );
      } else if (next.isSuccess && next.isSuccess != previous?.isSuccess) {
        // Go straight to home or show success since we didn't go to waiting page
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Depósito exitoso')),
        );
        context.go('/home');
      }
    });

    return Scaffold(
      backgroundColor: const Color(0xFFF0F4F8), // Light grey background
      appBar: AppBar(
        title: Text(
          widget.method,
          style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.black87),
        ),
        backgroundColor: const Color(0xFFF0F4F8),
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.black87),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Top Card (Method Info)
                Container(
                  padding: const EdgeInsets.symmetric(vertical: 24, horizontal: 16),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(12),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.05),
                        blurRadius: 10,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Column(
                    children: [
                      // Simulated Nequi Logo text
                      Text(
                        widget.method,
                        style: const TextStyle(
                          fontSize: 32,
                          fontWeight: FontWeight.w900,
                          color: Color(0xFF28004D), // Nequi purple
                        ),
                      ),
                      const SizedBox(height: 24),
                      Wrap(
                        alignment: WrapAlignment.center,
                        crossAxisAlignment: WrapCrossAlignment.center,
                        spacing: 16,
                        runSpacing: 8,
                        children: [
                          Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.access_time, size: 16, color: Colors.black54),
                              const SizedBox(width: 4),
                              const Text(
                                'Normalmente en 5 minutos',
                                style: TextStyle(color: Colors.black54, fontSize: 12, fontWeight: FontWeight.w600),
                              ),
                            ],
                          ),
                          Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.swap_vert, size: 16, color: Colors.black54),
                              const SizedBox(width: 4),
                              const Text(
                                '\$10.000 - \$10.000.000',
                                style: TextStyle(color: Colors.black54, fontSize: 12, fontWeight: FontWeight.w600),
                              ),
                            ],
                          ),
                        ],
                      )
                    ],
                  ),
                ).animate().fadeIn().slideY(begin: 0.1, end: 0),

                if (widget.method == 'PSE') ...[
                  const SizedBox(height: 16),
                  _BankSelector(
                    selectedBankCode: state.bankCode,
                    onBankSelected: (bank) =>
                        ref.read(depositProvider.notifier).setBankCode(bank.code),
                  ),
                ],

                const SizedBox(height: 16),

                // Bottom Card (Amount Input)
                Container(
                  padding: const EdgeInsets.all(24),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(12),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.05),
                        blurRadius: 10,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Column(
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.baseline,
                        textBaseline: TextBaseline.alphabetic,
                        children: [
                          const Text(
                            '\$ ',
                            style: TextStyle(
                              fontSize: 24,
                              fontWeight: FontWeight.bold,
                              color: Colors.black54,
                            ),
                          ),
                          IntrinsicWidth(
                            child: TextField(
                              controller: _amountController,
                              keyboardType: TextInputType.number,
                              style: const TextStyle(
                                fontSize: 48,
                                fontWeight: FontWeight.bold,
                                color: Color(0xFF6B7A99),
                              ),
                              decoration: const InputDecoration(
                                border: InputBorder.none,
                                hintText: '0',
                                hintStyle: TextStyle(
                                  color: Color(0xFF6B7A99),
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 32),
                      
                      // Quick Amount Buttons
                      Row(
                        children: [
                          Expanded(child: _buildAmountButton('20000', '20.000')),
                          const SizedBox(width: 8),
                          Expanded(child: _buildAmountButton('50000', '50.000')),
                          const SizedBox(width: 8),
                          Expanded(child: _buildAmountButton('100000', '100.000')),
                        ],
                      ),
                      
                      const SizedBox(height: 48),
                      
                      // Submit Button
                      SizedBox(
                        width: double.infinity,
                        height: 56,
                        child: ElevatedButton(
                          onPressed: state.isLoading
                            ? null
                            : () {
                                if (state.amount <= 0) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(content: Text('Ingresa un monto para depositar')),
                                  );
                                  return;
                                }
                                if (widget.method == 'PSE') {
                                  if (state.bankCode == null || state.bankCode!.isEmpty) {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      const SnackBar(content: Text('Selecciona tu banco para continuar')),
                                    );
                                    return;
                                  }
                                  ref.read(depositProvider.notifier).submitNativePseDeposit();
                                } else {
                                  ref.read(depositProvider.notifier).submitDeposit(widget.method);
                                }
                              },
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Theme.of(context).colorScheme.primary,
                            foregroundColor: Theme.of(context).colorScheme.onPrimary,
                            disabledBackgroundColor: Theme.of(context).colorScheme.primary.withOpacity(0.5),
                            disabledForegroundColor: Colors.white70,
                            elevation: 0,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(16),
                            ),
                          ),
                          child: state.isLoading
                            ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                            : const Text(
                                'DEPÓSITO',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w900,
                                  letterSpacing: 1.2,
                                ),
                              ),
                        ),
                      ),
                    ],
                  ),
                ).animate().fadeIn(delay: 100.ms).slideY(begin: 0.1, end: 0),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildAmountButton(String value, String display) {
    return ElevatedButton(
      onPressed: () => _setAmount(value),
      style: ElevatedButton.styleFrom(
        backgroundColor: const Color(0xFFF2F4F7), // Light grey
        foregroundColor: const Color(0xFF2C3545), // Dark text
        elevation: 0,
        padding: const EdgeInsets.symmetric(vertical: 16),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
        ),
      ),
      child: Text(
        '\$$display',
        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
      ),
    );
  }
}

/// Selector nativo de banco PSE: reemplaza la elección de banco que antes
/// solo ocurría dentro del checkout hosteado de Wompi por una lista propia,
/// alimentada por el catálogo real sincronizado en el backend.
class _BankSelector extends ConsumerWidget {
  const _BankSelector({required this.selectedBankCode, required this.onBankSelected});

  final String? selectedBankCode;
  final ValueChanged<BankModel> onBankSelected;

  void _openPicker(BuildContext context, WidgetRef ref) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => Container(
        constraints: BoxConstraints(maxHeight: MediaQuery.of(context).size.height * 0.75),
        decoration: BoxDecoration(
          color: Theme.of(context).scaffoldBackgroundColor,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
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
            const SizedBox(height: 16),
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 24),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text('Selecciona tu banco', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
              ),
            ),
            const SizedBox(height: 12),
            Flexible(
              child: Consumer(
                builder: (context, ref, _) {
                  final banksAsync = ref.watch(banksProvider('PSE'));
                  return banksAsync.when(
                    loading: () => const Padding(
                      padding: EdgeInsets.all(24),
                      child: Center(child: CircularProgressIndicator()),
                    ),
                    error: (error, _) => Padding(
                      padding: const EdgeInsets.all(24),
                      child: Text('No fue posible cargar la lista de bancos: $error'),
                    ),
                    data: (banks) {
                      if (banks.isEmpty) {
                        return const Padding(
                          padding: EdgeInsets.all(24),
                          child: Text('No hay bancos PSE disponibles por el momento'),
                        );
                      }
                      return ListView.separated(
                        shrinkWrap: true,
                        padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                        itemCount: banks.length,
                        separatorBuilder: (_, __) => const Divider(height: 1),
                        itemBuilder: (context, index) {
                          final bank = banks[index];
                          return ListTile(
                            leading: _bankLogo(bank.name),
                            title: Text(bank.name),
                            onTap: () {
                              onBankSelected(bank);
                              Navigator.pop(context);
                            },
                          );
                        },
                      );
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Insignia tipográfica del banco (colores/iniciales asociadas a la marca,
  /// no un logo oficial) — mismo criterio ya usado para Nequi/Bre-B/PSE en
  /// el resto de la app: evita depender de assets de imagen con derechos
  /// de marca de terceros, manteniendo igual la identificación visual.
  static const Map<String, _BankBadge> _bankBadges = {
    'bancolombia': _BankBadge(Color(0xFFFFD100), Color(0xFF002855), 'Bc'),
    'davivienda': _BankBadge(Color(0xFFEE3831), Colors.white, 'Dv'),
    'bbva': _BankBadge(Color(0xFF004481), Colors.white, 'BBVA'),
    'bogotá': _BankBadge(Color(0xFFDA291C), Colors.white, 'BB'),
    'occidente': _BankBadge(Color(0xFF006341), Colors.white, 'BO'),
    'popular': _BankBadge(Color(0xFFC8102E), Colors.white, 'BP'),
    'caja social': _BankBadge(Color(0xFFF58220), Colors.white, 'CS'),
    'scotiabank': _BankBadge(Color(0xFFEC111A), Colors.white, 'SC'),
    'colpatria': _BankBadge(Color(0xFFEC111A), Colors.white, 'SC'),
    'itaú': _BankBadge(Color(0xFFEC7000), Colors.white, 'I'),
    'itau': _BankBadge(Color(0xFFEC7000), Colors.white, 'I'),
    'av villas': _BankBadge(Color(0xFFFBB034), Color(0xFF5B3A00), 'AV'),
    'agrario': _BankBadge(Color(0xFF2E7D32), Colors.white, 'BA'),
    'bancoomeva': _BankBadge(Color(0xFF00A19A), Colors.white, 'Bo'),
    'nequi': _BankBadge(Color(0xFFE10098), Colors.white, 'Nq'),
    'daviplata': _BankBadge(Color(0xFFED1C24), Colors.white, 'DP'),
  };

  Widget _bankLogo(String bankName) {
    final normalized = bankName.toLowerCase();
    _BankBadge? badge;
    for (final entry in _bankBadges.entries) {
      if (normalized.contains(entry.key)) {
        badge = entry.value;
        break;
      }
    }

    if (badge == null) {
      return CircleAvatar(
        backgroundColor: Colors.grey.shade200,
        child: const Icon(Icons.account_balance_outlined, color: Colors.black54, size: 20),
      );
    }

    return Container(
      width: 40,
      height: 40,
      alignment: Alignment.center,
      decoration: BoxDecoration(color: badge.background, borderRadius: BorderRadius.circular(10)),
      child: Text(
        badge.label,
        style: TextStyle(color: badge.foreground, fontWeight: FontWeight.w900, fontSize: 13),
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selectedName = selectedBankCode == null
        ? null
        : ref.watch(banksProvider('PSE')).maybeWhen(
              data: (banks) {
                for (final bank in banks) {
                  if (bank.code == selectedBankCode) return bank.name;
                }
                return null;
              },
              orElse: () => null,
            );

    return InkWell(
      onTap: () => _openPicker(context, ref),
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: selectedBankCode == null ? Colors.orange.shade300 : Colors.grey.shade300,
          ),
        ),
        child: Row(
          children: [
            const Icon(Icons.account_balance_outlined, color: Colors.black54),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                selectedName ?? 'Selecciona tu banco',
                style: TextStyle(
                  fontWeight: FontWeight.w600,
                  color: selectedName == null ? Colors.black54 : Colors.black87,
                ),
              ),
            ),
            const Icon(Icons.chevron_right, color: Colors.black54),
          ],
        ),
      ),
    );
  }
}

class _BankBadge {
  const _BankBadge(this.background, this.foreground, this.label);

  final Color background;
  final Color foreground;
  final String label;
}
