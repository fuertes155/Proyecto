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

/// Monto mínimo de crédito que ofrece la cooperativa, independiente del perfil
/// de riesgo del usuario (coincide con el piso validado en el backend).
const double _kMinLoanAmount = 500000;

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
  double _annualRate = 0.24;
  bool _isSubmitting = false;
  bool _hasAcceptedHabeasData = false;
  bool _boundsInitialized = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _requestConsentAndLoad());
  }

  @override
  void dispose() {
    _purposeController.dispose();
    super.dispose();
  }

  Future<void> _requestConsentAndLoad() async {
    final accepted = await _showHabeasDataConsentDialog();
    if (!mounted) return;
    if (accepted != true) {
      context.pop();
      return;
    }
    setState(() => _hasAcceptedHabeasData = true);
    ref.read(loanEligibilityProvider.notifier).fetch();
  }

  Future<bool?> _showHabeasDataConsentDialog() {
    return showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        title: const Text('Autorización Habeas Data'),
        content: const Text(
          'Para calcular el monto, plazo y tasa que puedes solicitar, necesitamos '
          'consultar tu historial crediticio en la central de riesgo (DataCrédito). '
          '¿Autorizas esta consulta?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('No autorizo'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Autorizo la consulta'),
          ),
        ],
      ),
    );
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
              hasAcceptedHabeasData: _hasAcceptedHabeasData,
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
    final eligibilityState = ref.watch(loanEligibilityProvider);

    // La primera vez que llegan los límites reales, encuadramos los valores
    // por defecto del simulador dentro de lo que el usuario puede pedir.
    ref.listen<AsyncValue<LoanEligibility?>>(loanEligibilityProvider, (previous, next) {
      final eligibility = next.value;
      if (eligibility == null || !eligibility.approved || _boundsInitialized) return;
      if (eligibility.maxAmount < _kMinLoanAmount) return;
      setState(() {
        _boundsInitialized = true;
        _amount = _amount.clamp(_kMinLoanAmount, eligibility.maxAmount).toDouble();
        _termMonths = _termMonths.clamp(6, eligibility.maxTermMonths).toInt();
        _annualRate = eligibility.annualInterestRate;
      });
    });

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
                      eligibilityState.value?.approved == true
                          ? 'Según tu perfil (${eligibilityState.value!.tierLabel}) en DataCrédito'
                          : 'Encuentra la cuota ideal para ti',
                      style: theme.textTheme.titleMedium?.copyWith(
                        color: Colors.white70,
                      ),
                    ),
                    const Spacer(),
                    if (eligibilityState.value?.approved == true)
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
              child: eligibilityState.when(
                data: (eligibility) => _buildBody(theme, eligibility),
                loading: () => _buildLoading(),
                error: (e, st) => _buildError(theme, e),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLoading() {
    return const Padding(
      padding: EdgeInsets.symmetric(vertical: 64),
      child: Column(
        children: [
          CircularProgressIndicator(),
          SizedBox(height: 24),
          Text('Consultando tu perfil crediticio en DataCrédito...', textAlign: TextAlign.center),
        ],
      ),
    );
  }

  Widget _buildError(ThemeData theme, Object error) {
    return Column(
      children: [
        const Icon(Icons.error_outline, color: Colors.red, size: 48),
        const SizedBox(height: 16),
        Text(ErrorMapper.toUserMessage(error), textAlign: TextAlign.center),
        const SizedBox(height: 24),
        AccessibleButton(
          label: 'Reintentar',
          onPressed: () => ref.read(loanEligibilityProvider.notifier).fetch(),
        ),
      ],
    );
  }

  Widget _buildBody(ThemeData theme, LoanEligibility? eligibility) {
    if (eligibility == null) {
      // Aún no se ha aceptado Habeas Data / no se ha disparado la consulta.
      return _buildLoading();
    }

    if (!eligibility.approved || eligibility.maxAmount < _kMinLoanAmount) {
      return _buildNotEligible(theme, eligibility);
    }

    return _buildCalculatorForm(theme, eligibility);
  }

  Widget _buildNotEligible(ThemeData theme, LoanEligibility eligibility) {
    return Column(
      children: [
        const Icon(Icons.info_outline, color: AppTheme.primaryColor, size: 48),
        const SizedBox(height: 16),
        Text(
          'Por ahora no puedes solicitar un préstamo',
          style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 8),
        Text(
          eligibility.reason ??
              'Tu perfil de riesgo (${eligibility.tierLabel}) o tu saldo de ahorro actual no alcanzan el monto mínimo de crédito.',
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 24),
        AccessibleButton(
          label: 'Volver',
          onPressed: () => context.pop(),
        ),
      ],
    );
  }

  Widget _buildCalculatorForm(ThemeData theme, LoanEligibility eligibility) {
    return Form(
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
              min: _kMinLoanAmount,
              max: eligibility.maxAmount,
              divisions: 99,
              onChanged: (v) => setState(() => _amount = (v / 10000).round() * 10000),
            ),
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(formatCop(_kMinLoanAmount), style: theme.textTheme.bodySmall),
              Text(formatCop(eligibility.maxAmount), style: theme.textTheme.bodySmall),
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
              max: eligibility.maxTermMonths.toDouble(),
              divisions: math.max(1, eligibility.maxTermMonths - 6),
              onChanged: (v) => setState(() => _termMonths = v.toInt()),
            ),
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('6 meses'),
              Text('${eligibility.maxTermMonths} meses'),
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
    );
  }
}
