import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/utils/currency_formatter.dart';
import '../../../../core/widgets/accessible_button.dart';
import '../../data/datasources/solidarity_remote_datasource.dart';
import '../../data/models/solidarity_models.dart';
import '../providers/solidarity_provider.dart';

class SolidarityGroupDetailPage extends ConsumerStatefulWidget {
  const SolidarityGroupDetailPage({super.key, required this.groupId});

  final String groupId;

  @override
  ConsumerState<SolidarityGroupDetailPage> createState() => _SolidarityGroupDetailPageState();
}

class _SolidarityGroupDetailPageState extends ConsumerState<SolidarityGroupDetailPage> {
  @override
  Widget build(BuildContext context) {
    final groupAsync = ref.watch(solidarityGroupDetailProvider(widget.groupId));
    final loansAsync = ref.watch(solidarityLoansProvider(widget.groupId));
    final txAsync = ref.watch(solidarityTransactionsProvider(widget.groupId));

    return Scaffold(
      appBar: AppBar(title: const Text('Grupo solidario')),
      body: groupAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Error: $e')),
        data: (group) => SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(group.name, style: Theme.of(context).textTheme.headlineLarge),
              if (group.description != null) Text(group.description!),
              const SizedBox(height: 8),
              Text('Código: ${group.inviteCode}', style: const TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(height: 24),
              Text(formatCop(group.poolBalance), style: const TextStyle(fontSize: 32, fontWeight: FontWeight.bold)),
              Text('Préstamo máximo: ${formatCop(group.maxLoanAmount)}'),
              const SizedBox(height: 24),
              AccessibleButton(
                label: 'Aportar al fondo',
                onPressed: () => _showContributeDialog(group),
              ),
              const SizedBox(height: 12),
              AccessibleButton(
                label: 'Solicitar micropréstamo',
                onPressed: () => _showLoanRequestDialog(group),
              ),
              const SizedBox(height: 32),
              const Text('Préstamos', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600)),
              loansAsync.when(
                loading: () => const CircularProgressIndicator(),
                error: (e, _) => Text('$e'),
                data: (loans) => loans.isEmpty
                    ? const Text('Sin préstamos')
                    : Column(
                        children: loans.map((loan) => _LoanTile(
                          group: group,
                          loan: loan,
                          groupId: widget.groupId,
                          onUpdated: _refresh,
                        )).toList(),
                      ),
              ),
              const SizedBox(height: 24),
              const Text('Movimientos', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600)),
              txAsync.when(
                loading: () => const CircularProgressIndicator(),
                error: (e, _) => Text('$e'),
                data: (txs) => txs.isEmpty
                    ? const Text('Sin movimientos')
                    : Column(
                        children: txs
                            .map((tx) => ListTile(
                                  title: Text('${tx.typeLabel}: ${formatCop(tx.amount)}'),
                                  subtitle: Text(tx.description),
                                ))
                            .toList(),
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _refresh() {
    ref.invalidate(solidarityGroupDetailProvider(widget.groupId));
    ref.invalidate(solidarityLoansProvider(widget.groupId));
    ref.invalidate(solidarityTransactionsProvider(widget.groupId));
  }

  Future<void> _showContributeDialog(SolidarityGroup group) async {
    final controller = TextEditingController();
    final amount = await showDialog<double>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Aportar al fondo'),
        content: TextField(
          controller: controller,
          decoration: InputDecoration(
            labelText: 'Monto (mín. ${formatCop(group.minContribution)})',
            prefixText: '\$ ',
          ),
          keyboardType: TextInputType.number,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancelar')),
          TextButton(
            onPressed: () => Navigator.pop(ctx, double.tryParse(controller.text)),
            child: const Text('Aportar'),
          ),
        ],
      ),
    );
    if (amount == null) return;
    try {
      await ref.read(solidarityRemoteDataSourceProvider).contribute(
            widget.groupId,
            ContributeToPoolRequest(amount: amount),
          );
      _refresh();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    }
  }

  Future<void> _showLoanRequestDialog(SolidarityGroup group) async {
    final amountController = TextEditingController();
    final purposeController = TextEditingController();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Solicitar micropréstamo'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: amountController,
              decoration: InputDecoration(
                labelText: 'Monto (máx. ${formatCop(group.maxLoanAmount)})',
                prefixText: '\$ ',
              ),
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
            ),
            TextField(
              controller: purposeController,
              decoration: const InputDecoration(labelText: 'Propósito'),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancelar')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Solicitar')),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await ref.read(solidarityRemoteDataSourceProvider).requestLoan(
            widget.groupId,
            RequestMicroLoanRequest(
              amount: double.parse(amountController.text),
              purpose: purposeController.text,
              termMonths: 6,
            ),
          );
      _refresh();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    }
  }
}

class _LoanTile extends ConsumerWidget {
  const _LoanTile({
    required this.group,
    required this.loan,
    required this.groupId,
    required this.onUpdated,
  });

  final SolidarityGroup group;
  final MicroLoan loan;
  final String groupId;
  final VoidCallback onUpdated;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Card(
      child: ListTile(
        title: Text('${formatCop(loan.amount)} - ${loan.statusLabel}'),
        subtitle: Text(loan.purpose),
        trailing: group.isAdmin && loan.status == 'PENDING'
            ? Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton(
                    icon: const Icon(Icons.check, color: Colors.green),
                    onPressed: () => _review(ref, true),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, color: Colors.red),
                    onPressed: () => _review(ref, false),
                  ),
                ],
              )
            : loan.status == 'DISBURSED'
                ? IconButton(
                    icon: const Icon(Icons.payment),
                    onPressed: () => _showInstallments(context, ref),
                  )
                : null,
      ),
    );
  }

  Future<void> _review(WidgetRef ref, bool approved) async {
    await ref.read(solidarityRemoteDataSourceProvider).reviewLoan(
          groupId,
          loan.id,
          ReviewMicroLoanRequest(approved: approved),
        );
    onUpdated();
  }

  Future<void> _showInstallments(BuildContext context, WidgetRef ref) async {
    final installments = await ref.read(solidarityRemoteDataSourceProvider).listInstallments(groupId, loan.id);
    if (!context.mounted) return;
    showModalBottomSheet(
      context: context,
      builder: (ctx) => ListView(
        children: installments
            .map((i) => ListTile(
                  title: Text('Cuota ${i.installmentNumber}: ${formatCop(i.totalAmount)}'),
                  subtitle: Text('Vence: ${i.dueDate}'),
                  trailing: i.status == 'PENDING'
                      ? TextButton(
                          onPressed: () async {
                            await ref.read(solidarityRemoteDataSourceProvider).payInstallment(
                                  groupId, loan.id, i.id);
                            Navigator.pop(ctx);
                            onUpdated();
                          },
                          child: const Text('Pagar'),
                        )
                      : const Icon(Icons.check, color: Colors.green),
                ))
            .toList(),
      ),
    );
  }
}
