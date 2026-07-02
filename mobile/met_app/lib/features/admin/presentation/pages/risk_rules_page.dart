import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/admin_provider.dart';
import '../../data/models/admin_models.dart';

class RiskRulesPage extends ConsumerWidget {
  const RiskRulesPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final rulesState = ref.watch(riskRulesProvider);
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Row(children: [
          Icon(Icons.policy_rounded, color: primaryColor, size: 24),
          const SizedBox(width: 10),
          const Expanded(child: Text('Reglas de Riesgo',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold))),
        ]),
        actions: [
          IconButton(
            icon: Icon(Icons.refresh_rounded, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)),
            onPressed: () => ref.read(riskRulesProvider.notifier).load(),
          ),
        ],
      ),
      body: rulesState.when(
        loading: () =>
            Center(child: CircularProgressIndicator(color: primaryColor)),
        error: (e, _) => Center(
            child: Text('Error: $e', style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)))),
        data: (rules) {
          if (rules.isEmpty) {
            return Center(
                child: Text('No hay reglas configuradas',
                    style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5))));
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: rules.length,
            itemBuilder: (context, i) =>
                _RiskRuleCard(rule: rules[i], color: primaryColor),
          );
        },
      ),
    );
  }
}

class _RiskRuleCard extends ConsumerWidget {
  const _RiskRuleCard({required this.rule, required this.color});
  final RiskRule rule;
  final Color color;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.05 : 0.03),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
            color: rule.activo ? color.withOpacity(0.5) : Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.12 : 0.05)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: rule.activo
                        ? color.withOpacity(0.2)
                        : Theme.of(context).colorScheme.onSurface.withOpacity(isDark ? 0.05 : 0.03)),
                child: Icon(Icons.security_rounded,
                    color: rule.activo ? color : Theme.of(context).colorScheme.onSurface.withOpacity(0.4), size: 22),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(rule.nombre,
                        style: TextStyle(
                            color: Theme.of(context).colorScheme.onSurface,
                            fontWeight: FontWeight.bold,
                            fontSize: 16)),
                    if (rule.descripcion != null)
                      Text(rule.descripcion!,
                          style: TextStyle(
                              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6), fontSize: 12)),
                  ],
                ),
              ),
              Switch(
                value: rule.activo,
                activeThumbColor: color,
                onChanged: (val) async {
                  try {
                    await ref
                        .read(riskRulesProvider.notifier)
                        .toggle(rule.id, val);
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                        content: Text(val ? 'Regla activada' : 'Regla desactivada'),
                        backgroundColor: val ? color : Colors.grey[800],
                        behavior: SnackBarBehavior.floating,
                      ));
                    }
                  } catch (e) {
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                        content: Text('Error: $e'),
                        backgroundColor: Theme.of(context).colorScheme.error,
                        behavior: SnackBarBehavior.floating,
                      ));
                    }
                  }
                },
              ),
            ],
          ),
          const SizedBox(height: 14),
          _DetailRow('Condición', rule.condicion),
          const SizedBox(height: 6),
          _DetailRow('Acción', rule.accion),
        ],
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  const _DetailRow(this.label, this.value);
  final String label;
  final String value;

  String _formatValue(String val) {
    try {
      final map = jsonDecode(val) as Map<String, dynamic>;
      final field = map['field'];
      final value = map['value'];
      final operator = map['operator'];
      
      String opText = operator == 'gt' ? 'mayor a' : operator == 'lt' ? 'menor a' : operator == 'eq' ? 'igual a' : operator.toString();
      String fieldText = field == 'amount' ? 'Monto' : field == 'failed_attempts' ? 'Intentos fallidos' : field.toString();
      String extra = map.containsKey('window_minutes') ? ' en ${map['window_minutes']} minutos' : '';
      
      if (field == 'amount') {
        return '$fieldText $opText \$${value.toString().replaceAll(RegExp(r'\B(?=(\d{3})+(?!\d))'), ".")}$extra';
      }
      return '$fieldText $opText $value$extra';
    } catch (_) {
      // If it's not JSON, return as is
      return val == 'REVIEW' ? 'Revisión Manual' : val == 'BLOCK' ? 'Bloqueo Automático' : val;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(Icons.code_rounded, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4), size: 16),
        const SizedBox(width: 8),
        Text(label,
            style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5), fontSize: 13)),
        const SizedBox(width: 8),
        Expanded(
          child: Text(_formatValue(value),
              textAlign: TextAlign.right,
              style: TextStyle(
                  color: Theme.of(context).colorScheme.onSurface, fontWeight: FontWeight.w500, fontSize: 13)),
        ),
      ],
    );
  }
}
