import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../providers/deposit_provider.dart';

class DepositPage extends ConsumerStatefulWidget {
  final String method;
  const DepositPage({super.key, required this.method});

  @override
  ConsumerState<DepositPage> createState() => _DepositPageState();
}

class _DepositPageState extends ConsumerState<DepositPage> {
  final _amountController = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(depositProvider.notifier).reset();
      ref.read(depositProvider.notifier).setMethod(widget.method);
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
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Icon(Icons.access_time, size: 16, color: Colors.black54),
                          const SizedBox(width: 4),
                          const Text(
                            'Normalmente en 5 minutos',
                            style: TextStyle(color: Colors.black54, fontSize: 12, fontWeight: FontWeight.w600),
                          ),
                          const SizedBox(width: 16),
                          const Icon(Icons.swap_vert, size: 16, color: Colors.black54),
                          const SizedBox(width: 4),
                          const Text(
                            '\$10.000 - \$10.000.000',
                            style: TextStyle(color: Colors.black54, fontSize: 12, fontWeight: FontWeight.w600),
                          ),
                        ],
                      )
                    ],
                  ),
                ).animate().fadeIn().slideY(begin: 0.1, end: 0),
                
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
                          Expanded(child: _buildAmountButton('20000')),
                          const SizedBox(width: 8),
                          Expanded(child: _buildAmountButton('22000')),
                          const SizedBox(width: 8),
                          Expanded(child: _buildAmountButton('50000')),
                        ],
                      ),
                      
                      const SizedBox(height: 48),
                      
                      // Submit Button
                      SizedBox(
                        width: double.infinity,
                        height: 56,
                        child: ElevatedButton(
                          onPressed: state.amount > 0 ? () {
                            // Navigate to waiting screen (Simulating Webhook flow)
                            context.push('/deposit/waiting', extra: {
                              'method': widget.method,
                              'amount': state.amount,
                            });
                          } : null,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFFE8E8E8), // Light grey when disabled/normal
                            foregroundColor: Colors.black26,
                            disabledBackgroundColor: const Color(0xFFF2F4F7),
                            elevation: 0,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(8),
                            ),
                          ).copyWith(
                            backgroundColor: WidgetStateProperty.resolveWith<Color>((states) {
                              if (states.contains(WidgetState.disabled)) return const Color(0xFFF2F4F7);
                              return const Color(0xFFE8E8E8); // Still grey as per screenshot
                            }),
                          ),
                          child: const Text(
                            'DEPÓSITO',
                            style: TextStyle(
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

  Widget _buildAmountButton(String amount) {
    return ElevatedButton(
      onPressed: () => _setAmount(amount),
      style: ElevatedButton.styleFrom(
        backgroundColor: const Color(0xFF2C3545), // Dark grey/blue
        foregroundColor: Colors.white,
        elevation: 0,
        padding: const EdgeInsets.symmetric(vertical: 16),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
        ),
      ),
      child: Text(
        '\$$amount',
        style: const TextStyle(fontWeight: FontWeight.bold),
      ),
    );
  }
}
