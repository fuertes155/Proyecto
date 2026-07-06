import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../providers/pin_recovery_provider.dart';

class PinRecoveryPage extends ConsumerStatefulWidget {
  const PinRecoveryPage({super.key});

  @override
  ConsumerState<PinRecoveryPage> createState() => _PinRecoveryPageState();
}

class _PinRecoveryPageState extends ConsumerState<PinRecoveryPage> {
  final _documentController = TextEditingController();
  final _otpController = TextEditingController();
  final _newPinController = TextEditingController();
  final _confirmPinController = TextEditingController();
  
  String _selectedDocType = 'CC';

  @override
  void dispose() {
    _documentController.dispose();
    _otpController.dispose();
    _newPinController.dispose();
    _confirmPinController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(pinRecoveryProvider);
    final notifier = ref.read(pinRecoveryProvider.notifier);

    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () {
            if (state.currentStep > 0) {
              notifier.previousStep();
            } else {
              context.pop();
            }
          },
        ),
        title: const Text('Recuperar PIN', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
      ),
      body: SafeArea(
        child: Column(
          children: [
            // Progress Indicator
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16.0),
              child: Row(
                children: List.generate(3, (index) {
                  return Expanded(
                    child: Container(
                      height: 4,
                      margin: EdgeInsets.only(right: index < 2 ? 8 : 0),
                      decoration: BoxDecoration(
                        color: index <= state.currentStep ? const Color(0xFF4CAF50) : Colors.white24,
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  );
                }),
              ),
            ),

            if (state.error != null)
              Container(
                margin: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.red.withOpacity(0.9),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.error_outline, color: Colors.white),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        state.error!,
                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w500),
                      ),
                    ),
                  ],
                ),
              ),

            Expanded(
              child: AnimatedSwitcher(
                duration: const Duration(milliseconds: 300),
                child: _buildCurrentStep(state, notifier),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCurrentStep(PinRecoveryState state, PinRecoveryNotifier notifier) {
    switch (state.currentStep) {
      case 0:
        return _buildStep0(state, notifier);
      case 1:
        return _buildStep1(state, notifier);
      case 2:
        return _buildStep2(state, notifier);
      default:
        return const SizedBox();
    }
  }

  Widget _buildStep0(PinRecoveryState state, PinRecoveryNotifier notifier) {
    return Padding(
      key: const ValueKey(0),
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '¿Olvidaste tu PIN?',
            style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.white),
          ),
          const SizedBox(height: 8),
          const Text(
            'Ingresa tu tipo y número de documento para enviarte un código al correo registrado.',
            style: TextStyle(color: Colors.white70, fontSize: 16),
          ),
          const SizedBox(height: 32),
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12),
                decoration: BoxDecoration(
                  color: Colors.white12,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: DropdownButtonHideUnderline(
                  child: DropdownButton<String>(
                    value: _selectedDocType,
                    dropdownColor: const Color(0xFF1E1E1E),
                    style: const TextStyle(color: Colors.white, fontSize: 16),
                    icon: const Icon(Icons.arrow_drop_down, color: Colors.white70),
                    items: const [
                      DropdownMenuItem(value: 'CC', child: Text('CC')),
                      DropdownMenuItem(value: 'CE', child: Text('CE')),
                      DropdownMenuItem(value: 'PASSPORT', child: Text('PASSPORT')),
                    ],
                    onChanged: (val) {
                      if (val != null) setState(() => _selectedDocType = val);
                    },
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextFormField(
                  controller: _documentController,
                  style: const TextStyle(color: Colors.white),
                  keyboardType: TextInputType.number,
                  decoration: InputDecoration(
                    labelText: 'Número de documento',
                    labelStyle: const TextStyle(color: Colors.white54),
                    filled: true,
                    fillColor: Colors.white12,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: BorderSide.none,
                    ),
                  ),
                ),
              ),
            ],
          ),
          const Spacer(),
          SizedBox(
            width: double.infinity,
            height: 56,
            child: ElevatedButton(
              onPressed: state.isLoading
                  ? null
                  : () {
                      if (_documentController.text.isNotEmpty) {
                        notifier.requestRecovery(_selectedDocType, _documentController.text);
                      }
                    },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF4CAF50),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
              ),
              child: state.isLoading
                  ? const CircularProgressIndicator(color: Colors.white)
                  : const Text('Solicitar Código', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStep1(PinRecoveryState state, PinRecoveryNotifier notifier) {
    return Padding(
      key: const ValueKey(1),
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Revisa tu correo',
            style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.white),
          ),
          const SizedBox(height: 8),
          const Text(
            'Hemos enviado un código de 6 dígitos a tu correo electrónico.',
            style: TextStyle(color: Colors.white70, fontSize: 16),
          ),
          const SizedBox(height: 32),
          TextFormField(
            controller: _otpController,
            style: const TextStyle(color: Colors.white, fontSize: 24, letterSpacing: 8),
            textAlign: TextAlign.center,
            keyboardType: TextInputType.number,
            maxLength: 6,
            decoration: InputDecoration(
              counterText: '',
              hintText: '000000',
              hintStyle: const TextStyle(color: Colors.white24),
              filled: true,
              fillColor: Colors.white12,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide.none,
              ),
            ),
          ),
          const Spacer(),
          SizedBox(
            width: double.infinity,
            height: 56,
            child: ElevatedButton(
              onPressed: () {
                if (_otpController.text.length == 6) {
                  notifier.nextStep(); // Only visual validation, backend validated on step 2
                }
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF4CAF50),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
              ),
              child: const Text('Validar Código', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStep2(PinRecoveryState state, PinRecoveryNotifier notifier) {
    return Padding(
      key: const ValueKey(2),
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Crea tu nuevo PIN',
            style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.white),
          ),
          const SizedBox(height: 8),
          const Text(
            'Asegúrate de recordarlo y no compartirlo con nadie.',
            style: TextStyle(color: Colors.white70, fontSize: 16),
          ),
          const SizedBox(height: 32),
          TextFormField(
            controller: _newPinController,
            style: const TextStyle(color: Colors.white, fontSize: 24, letterSpacing: 16),
            textAlign: TextAlign.center,
            keyboardType: TextInputType.number,
            obscureText: true,
            maxLength: 4,
            decoration: InputDecoration(
              labelText: 'Nuevo PIN (4 dígitos)',
              labelStyle: const TextStyle(color: Colors.white54, fontSize: 14, letterSpacing: 0),
              counterText: '',
              filled: true,
              fillColor: Colors.white12,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide.none,
              ),
            ),
          ),
          const SizedBox(height: 16),
          TextFormField(
            controller: _confirmPinController,
            style: const TextStyle(color: Colors.white, fontSize: 24, letterSpacing: 16),
            textAlign: TextAlign.center,
            keyboardType: TextInputType.number,
            obscureText: true,
            maxLength: 4,
            decoration: InputDecoration(
              labelText: 'Confirmar Nuevo PIN',
              labelStyle: const TextStyle(color: Colors.white54, fontSize: 14, letterSpacing: 0),
              counterText: '',
              filled: true,
              fillColor: Colors.white12,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide.none,
              ),
            ),
          ),
          const Spacer(),
          SizedBox(
            width: double.infinity,
            height: 56,
            child: ElevatedButton(
              onPressed: state.isLoading
                  ? null
                  : () async {
                      if (_newPinController.text != _confirmPinController.text) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Los PIN no coinciden', style: TextStyle(color: Colors.white)), backgroundColor: Colors.red),
                        );
                        return;
                      }
                      if (_newPinController.text.length == 4) {
                        final success = await notifier.resetPin(_otpController.text, _newPinController.text);
                        if (success) {
                          if (context.mounted) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('PIN actualizado correctamente', style: TextStyle(color: Colors.white)), backgroundColor: Colors.green),
                            );
                            context.go('/login');
                          }
                        }
                      }
                    },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF4CAF50),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
              ),
              child: state.isLoading
                  ? const CircularProgressIndicator(color: Colors.white)
                  : const Text('Actualizar PIN', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
            ),
          ),
        ],
      ),
    );
  }
}
