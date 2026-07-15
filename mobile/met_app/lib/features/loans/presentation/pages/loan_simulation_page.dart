import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_animate/flutter_animate.dart';

import '../../../../core/theme/app_theme.dart';
import '../../../../core/utils/currency_formatter.dart';
import '../../../../core/utils/error_mapper.dart';
import '../../../../core/widgets/accessible_button.dart';
import '../../data/datasources/loan_remote_datasource.dart';
import '../../data/datasources/legal_remote_datasource.dart';
import '../../data/models/loan_models.dart';
import '../providers/loan_provider.dart';

class LoanSimulationPage extends ConsumerStatefulWidget {
  const LoanSimulationPage({super.key});

  @override
  ConsumerState<LoanSimulationPage> createState() => _LoanSimulationPageState();
}

class _LoanSimulationPageState extends ConsumerState<LoanSimulationPage> {
  final _formKey = GlobalKey<FormState>();
  final _purposeController = TextEditingController();
  
  double _amount = 5000000;
  int _termMonths = 24;
  final double _annualRate = 0.24;
  bool _isSubmitting = false;

  @override
  void dispose() {
    _purposeController.dispose();
    super.dispose();
  }

  double get _monthlyPayment {
    final r = _annualRate / 12;
    if (r == 0) return _amount / _termMonths;
    final num = _amount * r * math.pow(1 + r, _termMonths);
    final den = math.pow(1 + r, _termMonths) - 1;
    return num / den;
  }

  Future<void> _simulateAndApply() async {
    if (_purposeController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Indica el propósito del préstamo')),
      );
      return;
    }
    setState(() => _isSubmitting = true);
    try {
      // 1. Solicitar Firma (OTP)
      final txId = await ref.read(legalRemoteDataSourceProvider).requestSignature();
      
      if (!mounted) return;
      setState(() => _isSubmitting = false);

      // 2. Mostrar Diálogo OTP
      final otp = await _showOtpDialog();
      if (otp == null || otp.isEmpty) return; // User cancelled

      setState(() => _isSubmitting = true);

      // 3. Confirmar Firma
      await ref.read(legalRemoteDataSourceProvider).confirmSignature(txId, otp);

      // 4. Crear Préstamo
      final app = await ref.read(loanRemoteDataSourceProvider).submitApplication(
            SubmitLoanApplicationRequest(
              amount: _amount,
              termMonths: _termMonths,
              annualInterestRate: _annualRate,
              purpose: _purposeController.text.trim(),
            ),
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('¡Solicitud y firma procesadas exitosamente!')),
      );
      context.push('/loans/applications/${app.id}');
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(ErrorMapper.toUserMessage(e)), backgroundColor: Colors.red));
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  Future<String?> _showOtpDialog() async {
    final otpController = TextEditingController();
    return showDialog<String>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) {
        return AlertDialog(
          title: const Text('Firma de Mandato Legal'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('Hemos enviado un código OTP a tu correo registrado para firmar electrónicamente el mandato de descuento de cuotas.'),
              const SizedBox(height: 16),
              TextField(
                controller: otpController,
                keyboardType: TextInputType.number,
                maxLength: 6,
                decoration: const InputDecoration(
                  labelText: 'Código OTP (6 dígitos)',
                  border: OutlineInputBorder(),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(null),
              child: const Text('Cancelar'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(ctx).pop(otpController.text),
              child: const Text('Firmar y Continuar'),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    
    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            expandedHeight: 320,
            pinned: true,
            flexibleSpace: FlexibleSpaceBar(
              background: Container(
                decoration: const BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [AppTheme.primaryColorLight, AppTheme.primaryColor],
                  ),
                ),
                padding: const EdgeInsets.fromLTRB(24, 80, 24, 24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Calcula tu Préstamo',
                      style: theme.textTheme.headlineMedium?.copyWith(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Encuentra la cuota ideal para ti',
                      style: theme.textTheme.titleMedium?.copyWith(
                        color: Colors.white70,
                      ),
                    ),
                    const Spacer(),
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: Colors.white.withOpacity(0.2)),
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Cuota estimada', style: TextStyle(color: Colors.white70)),
                              const SizedBox(height: 4),
                              Text(
                                formatCop(_monthlyPayment),
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 24,
                                  fontWeight: FontWeight.w900,
                                ),
                              ),
                            ],
                          ),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              const Text('Tasa EA', style: TextStyle(color: Colors.white70)),
                              const SizedBox(height: 4),
                              Text(
                                '${(_annualRate * 100).toStringAsFixed(1)}%',
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 20,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ).animate().fadeIn().slideY(begin: 0.2),
                  ],
                ),
              ),
            ),
            actions: [
              IconButton(
                icon: const Icon(Icons.list_alt, color: Colors.white),
                tooltip: 'Mis solicitudes',
                onPressed: () => context.push('/loans/applications'),
              ),
            ],
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.all(24.0),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text(
                      '¿Cuánto dinero necesitas?',
                      style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 16),
                    Center(
                      child: Text(
                        formatCop(_amount),
                        style: theme.textTheme.headlineLarge?.copyWith(
                          color: AppTheme.primaryColor,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ),
                    SliderTheme(
                      data: SliderTheme.of(context).copyWith(
                        activeTrackColor: AppTheme.primaryColor,
                        inactiveTrackColor: AppTheme.primaryColor.withOpacity(0.2),
                        thumbColor: AppTheme.primaryColor,
                        overlayColor: AppTheme.primaryColor.withOpacity(0.1),
                        trackHeight: 8,
                      ),
                      child: Slider(
                        value: _amount,
                        min: 500000,
                        max: 50000000,
                        divisions: 99,
                        onChanged: (v) => setState(() => _amount = (v / 10000).round() * 10000),
                      ),
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(formatCop(500000), style: theme.textTheme.bodySmall),
                        Text(formatCop(50000000), style: theme.textTheme.bodySmall),
                      ],
                    ),
                    const SizedBox(height: 32),
                    
                    Text(
                      '¿En cuánto tiempo quieres pagarlo?',
                      style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 16),
                    Center(
                      child: Text(
                        '$_termMonths meses',
                        style: theme.textTheme.headlineMedium?.copyWith(
                          color: AppTheme.primaryColor,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ),
                    SliderTheme(
                      data: SliderTheme.of(context).copyWith(
                        activeTrackColor: AppTheme.primaryColor,
                        inactiveTrackColor: AppTheme.primaryColor.withOpacity(0.2),
                        thumbColor: AppTheme.primaryColor,
                        overlayColor: AppTheme.primaryColor.withOpacity(0.1),
                        trackHeight: 8,
                      ),
                      child: Slider(
                        value: _termMonths.toDouble(),
                        min: 6,
                        max: 60,
                        divisions: 54,
                        onChanged: (v) => setState(() => _termMonths = v.toInt()),
                      ),
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text('6 meses', style: theme.textTheme.bodySmall),
                        Text('60 meses', style: theme.textTheme.bodySmall),
                      ],
                    ),
                    
                    const SizedBox(height: 32),
                    TextFormField(
                      controller: _purposeController,
                      decoration: InputDecoration(
                        labelText: 'Propósito del préstamo',
                        hintText: 'Ej: Remodelación, Viaje, Estudio...',
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        filled: true,
                        fillColor: theme.colorScheme.surfaceContainerHighest.withOpacity(0.3),
                      ),
                    ),
                    
                    const SizedBox(height: 48),
                    AccessibleButton(
                      label: 'Solicitar Préstamo',
                      isLoading: _isSubmitting,
                      onPressed: _simulateAndApply,
                    ).animate(delay: 200.ms).fadeIn().scale(),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
