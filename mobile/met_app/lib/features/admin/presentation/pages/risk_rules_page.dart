import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/admin_provider.dart';
import '../../data/models/admin_models.dart';

class RiskRulesPage extends ConsumerWidget {
  const RiskRulesPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final rulesState = ref.watch(riskRulesProvider);
    const primaryColor = Color(0xFF53A835); // Verde oscuro

    return Scaffold(
      backgroundColor: const Color(0xFF0D0D0D),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        foregroundColor: Colors.white,
        title: const Row(children: [
          Icon(Icons.policy_rounded, color: primaryColor, size: 24),
          SizedBox(width: 10),
          Expanded(child: Text('Reglas de Riesgo',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold))),
        ]),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded, color: Colors.white70),
            onPressed: () => ref.read(riskRulesProvider.notifier).load(),
          ),
        ],
      ),
      body: rulesState.when(
        loading: () =>
            const Center(child: CircularProgressIndicator(color: primaryColor)),
        error: (e, _) => Center(
            child: Text('Error: $e', style: const TextStyle(color: Colors.white70))),
        data: (rules) {
          if (rules.isEmpty) {
            return const Center(
                child: Text('No hay reglas configuradas',
                    style: TextStyle(color: Colors.white54)));
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
    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.05),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
            color: rule.activo ? color.withOpacity(0.5) : Colors.white12),
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
                        : Colors.white.withOpacity(0.05)),
                child: Icon(Icons.security_rounded,
                    color: rule.activo ? color : Colors.white38, size: 22),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(rule.nombre,
                        style: const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                            fontSize: 16)),
                    if (rule.descripcion != null)
                      Text(rule.descripcion!,
                          style: TextStyle(
                              color: Colors.white.withOpacity(0.6), fontSize: 12)),
                  ],
                ),
              ),
              Switch(
                value: rule.activo,
                activeColor: color,
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
                        backgroundColor: const Color(0xFFCF3232),
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

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(Icons.code_rounded, color: Colors.white38, size: 16),
        const SizedBox(width: 8),
        Text(label,
            style: TextStyle(color: Colors.white.withOpacity(0.5), fontSize: 13)),
        const Spacer(),
        Text(value,
            style: const TextStyle(
                color: Colors.white, fontWeight: FontWeight.w500, fontSize: 13)),
      ],
    );
  }
}
