import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';

class InvestmentBreakdownPage extends StatelessWidget {
  const InvestmentBreakdownPage({super.key});

  @override
  Widget build(BuildContext context) {
    // Mock data based on TEST 2
    final mockFractions = [
      {'borrower': 'Pedro Perez', 'amount': 10000.0, 'status': 'Activo'},
      {'borrower': 'Maria Gomez', 'amount': 8500.0, 'status': 'Activo'},
      {'borrower': 'Luis Ramirez', 'amount': 10000.0, 'status': 'En mora'},
      {'borrower': 'Fondo de Liquidez', 'amount': 1500.0, 'status': 'Disponible'},
    ];

    return Scaffold(
      backgroundColor: const Color(0xFFF0F4F8),
      appBar: AppBar(
        title: const Text('Distribución de Inversión', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: const Color(0xFFF0F4F8),
        elevation: 0,
        foregroundColor: Colors.black87,
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: mockFractions.length + 1,
        itemBuilder: (context, index) {
          if (index == 0) {
            return const Padding(
              padding: EdgeInsets.only(bottom: 24),
              child: Text(
                'Tu dinero se ha fraccionado dinámicamente y se encuentra fondeando los siguientes créditos en la cooperativa:',
                style: TextStyle(fontSize: 16, color: Colors.black54),
              ),
            );
          }

          final fraction = mockFractions[index - 1];
          final isLate = fraction['status'] == 'En mora';
          final isLiquidity = fraction['borrower'] == 'Fondo de Liquidez';

          return Container(
            margin: const EdgeInsets.only(bottom: 12),
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: isLate ? Colors.red.withOpacity(0.3) : Colors.transparent,
                width: 1,
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.05),
                  blurRadius: 5,
                  offset: const Offset(0, 2),
                )
              ],
            ),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: isLiquidity 
                        ? Colors.blue.withOpacity(0.1) 
                        : isLate 
                            ? Colors.red.withOpacity(0.1) 
                            : Colors.green.withOpacity(0.1),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    isLiquidity ? Icons.pool : Icons.person,
                    color: isLiquidity ? Colors.blue : isLate ? Colors.red : Colors.green,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Fondeando a ${fraction['borrower']}',
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Estado: ${fraction['status']}',
                        style: TextStyle(
                          color: isLate ? Colors.red : Colors.black54,
                          fontWeight: isLate ? FontWeight.bold : FontWeight.normal,
                          fontSize: 13,
                        ),
                      ),
                    ],
                  ),
                ),
                Text(
                  '\$${fraction['amount']}',
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Color(0xFF2C3545)),
                ),
              ],
            ),
          ).animate().fadeIn(delay: Duration(milliseconds: 100 * index)).slideX();
        },
      ),
    );
  }
}
