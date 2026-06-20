import 'dart:ui';
import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:local_auth/local_auth.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../data/models/auth_models.dart';
import '../providers/auth_provider.dart';

// MEJORA 7 — Código reusable
class _AnimatedEntrance extends StatelessWidget {
  final Widget child;
  final int delayMs;

  const _AnimatedEntrance({required this.child, required this.delayMs});

  @override
  Widget build(BuildContext context) {
    return FutureBuilder(
      future: Future.delayed(Duration(milliseconds: delayMs)),
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return Opacity(
            opacity: 0,
            child: child,
          );
        }
        return TweenAnimationBuilder<double>(
          tween: Tween(begin: 0.0, end: 1.0),
          duration: const Duration(milliseconds: 600),
          curve: Curves.easeOutCubic,
          builder: (context, value, child) {
            return Transform.translate(
              offset: Offset(0, 20 * (1 - value)),
              child: Opacity(
                opacity: value,
                child: child,
              ),
            );
          },
          child: child,
        );
      },
    );
  }
}

class _GlassCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding;
  final double borderRadius;

  const _GlassCard({
    required this.child,
    this.padding,
    this.borderRadius = 24,
  });

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(borderRadius),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 15, sigmaY: 15),
        child: Container(
          padding: padding ?? const EdgeInsets.all(32),
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(0.15),
            borderRadius: BorderRadius.circular(borderRadius),
            border: Border.all(color: Colors.white.withOpacity(0.3)),
          ),
          child: child,
        ),
      ),
    );
  }
}

class _PinIndicator extends StatelessWidget {
  final bool isFilled;
  final bool isError;

  const _PinIndicator({
    required this.isFilled,
    required this.isError,
  });

  @override
  Widget build(BuildContext context) {
    Color color;
    if (isError) {
      color = Colors.redAccent;
    } else if (isFilled) {
      color = const Color(0xFFFF9800);
    } else {
      color = Colors.transparent;
    }

    return AnimatedContainer(
      duration: const Duration(milliseconds: 200),
      width: 20,
      height: 20,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: color,
        border: Border.all(
          color: isError
              ? Colors.redAccent
              : (isFilled ? const Color(0xFFFF9800) : Colors.white.withOpacity(0.5)),
          width: 2,
        ),
      ),
    );
  }
}

class _NumericKey extends StatefulWidget {
  final String text;
  final IconData? icon;
  final VoidCallback onPressed;

  const _NumericKey({
    this.text = '',
    this.icon,
    required this.onPressed,
  });

  @override
  State<_NumericKey> createState() => _NumericKeyState();
}

class _NumericKeyState extends State<_NumericKey> with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 100),
    );
    _scaleAnimation = Tween<double>(begin: 1.0, end: 0.96).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _onTapDown(TapDownDetails details) {
    HapticFeedback.lightImpact();
    _controller.forward();
  }

  void _onTapUp(TapUpDetails details) {
    _controller.reverse();
    widget.onPressed();
  }

  void _onTapCancel() {
    _controller.reverse();
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: _onTapDown,
      onTapUp: _onTapUp,
      onTapCancel: _onTapCancel,
      child: ScaleTransition(
        scale: _scaleAnimation,
        child: Container(
          margin: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: Colors.white.withOpacity(0.1),
            border: Border.all(color: Colors.white.withOpacity(0.2)),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.1),
                blurRadius: 10,
                offset: const Offset(0, 4),
              )
            ],
          ),
          child: AspectRatio(
            aspectRatio: 1,
            child: Center(
              child: widget.icon != null
                  ? Icon(widget.icon, color: Colors.white, size: 28)
                  : Text(
                      widget.text,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 32,
                        fontWeight: FontWeight.w400,
                      ),
                    ),
            ),
          ),
        ),
      ),
    );
  }
}

class _BiometricButton extends StatefulWidget {
  final VoidCallback onPressed;

  const _BiometricButton({required this.onPressed});

  @override
  State<_BiometricButton> createState() => _BiometricButtonState();
}

class _BiometricButtonState extends State<_BiometricButton> {
  bool _isHovered = false;

  @override
  Widget build(BuildContext context) {
    return MouseRegion(
      onEnter: (_) => setState(() => _isHovered = true),
      onExit: (_) => setState(() => _isHovered = false),
      child: GestureDetector(
        onTap: () {
          HapticFeedback.mediumImpact();
          widget.onPressed();
        },
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          margin: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: Colors.white.withOpacity(_isHovered ? 0.2 : 0.1),
            border: Border.all(
              color: Colors.white.withOpacity(0.4),
              width: 1.5,
            ),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFFFF9800).withOpacity(_isHovered ? 0.5 : 0.2),
                blurRadius: _isHovered ? 20 : 10,
                spreadRadius: _isHovered ? 5 : 2,
              )
            ],
          ),
          child: const AspectRatio(
            aspectRatio: 1,
            child: Center(
              child: Icon(
                Icons.fingerprint,
                color: Colors.white,
                size: 36,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> with TickerProviderStateMixin {
  final _formKey = GlobalKey<FormState>();
  final _documentController = TextEditingController();
  final _pinController = TextEditingController();
  String _documentType = 'CC';
  bool _isLoading = false;
  bool _canCheckBiometrics = false;
  bool _rememberMe = false;
  String? _rawSavedDocument;
  final LocalAuthentication auth = LocalAuthentication();
  final _storage = const FlutterSecureStorage();
  
  bool _isPinError = false;
  late AnimationController _shakeController;
  late Animation<double> _shakeAnimation;

  bool get _isWeb => kIsWeb;

  @override
  void initState() {
    super.initState();
    _shakeController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    );
    _shakeAnimation = Tween<double>(begin: 0, end: 10).animate(
      CurvedAnimation(parent: _shakeController, curve: Curves.elasticIn),
    )..addStatusListener((status) {
        if (status == AnimationStatus.completed) {
          _shakeController.reset();
          setState(() => _isPinError = false);
        }
      });

    if (!_isWeb) {
      _checkBiometrics();
    } else {
      _canCheckBiometrics = false;
    }
    _loadSavedDocument();
  }

  Future<void> _loadSavedDocument() async {
    final savedDoc = await _storage.read(key: 'saved_document');
    if (savedDoc != null && savedDoc.isNotEmpty) {
      _rawSavedDocument = savedDoc;
      setState(() {
        _rememberMe = true;
        if (savedDoc.length > 4) {
          _documentController.text = '*' * (savedDoc.length - 4) +
              savedDoc.substring(savedDoc.length - 4);
        } else {
          _documentController.text = savedDoc;
        }
      });
    }
  }

  void _onKeypadPressed(String value) {
    if (_pinController.text.length < 4) {
      setState(() {
        _pinController.text += value;
        _isPinError = false;
      });
    }
  }

  void _onKeypadBackspace() {
    if (_pinController.text.isNotEmpty) {
      setState(() {
        _pinController.text =
            _pinController.text.substring(0, _pinController.text.length - 1);
        _isPinError = false;
      });
    }
  }

  void _triggerError() {
    setState(() => _isPinError = true);
    HapticFeedback.heavyImpact();
    _shakeController.forward();
  }

  Future<void> _checkBiometrics() async {
    if (_isWeb) return;

    bool canCheckBiometrics;
    try {
      canCheckBiometrics =
          await auth.canCheckBiometrics || await auth.isDeviceSupported();
    } on PlatformException catch (_) {
      canCheckBiometrics = false;
    }
    if (!mounted) return;
    setState(() {
      _canCheckBiometrics = canCheckBiometrics;
    });
  }

  @override
  void dispose() {
    _documentController.dispose();
    _pinController.dispose();
    _shakeController.dispose();
    super.dispose();
  }

  String _getErrorMessage(Object error) {
    return error.toString();
  }

  void _showFriendlyErrorFromErrorObject(Object error) {
    final errorMsg = _getErrorMessage(error);
    String friendlyMessage =
        'Ocurrió un error inesperado. Por favor, intenta de nuevo.';
    final lowerError = errorMsg.toLowerCase();

    if (lowerError.contains('rate') ||
        lowerError.contains('429') ||
        lowerError.contains('too many requests')) {
      friendlyMessage =
          'Demasiados intentos. Espera unos segundos e inténtalo de nuevo.';
    } else if (lowerError.contains('pin') || lowerError.contains('incorrect')) {
      friendlyMessage =
          'El PIN es incorrecto. Por favor, verifica e intenta de nuevo.';
      _triggerError(); // MEJORA 3: Trigger shake and error color
    } else if (lowerError.contains('not found') ||
        lowerError.contains('usuario')) {
      friendlyMessage = 'No encontramos un usuario con este documento.';
    } else if (lowerError.contains('401') ||
        lowerError.contains('unauthorized')) {
      friendlyMessage =
          'No autorizado. Verifica tus credenciales e inténtalo de nuevo.';
      _triggerError();
    }

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          '$friendlyMessage\n${errorMsg.length > 200 ? errorMsg.substring(0, 200) + '...' : errorMsg}',
        ),
        backgroundColor: Colors.redAccent,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  Future<void> _loginWithPin() async {
    if (!_formKey.currentState!.validate()) return;
    if (_pinController.text.length != 4) {
       _triggerError();
       return;
    }

    setState(() => _isLoading = true);

    String docToSend = _documentController.text.trim();
    if (docToSend.contains('*') && _rawSavedDocument != null) {
      docToSend = _rawSavedDocument!;
    }

    if (_rememberMe) {
      await _storage.write(key: 'saved_document', value: docToSend);
    } else {
      await _storage.delete(key: 'saved_document');
    }

    await ref.read(authStateProvider.notifier).loginWithPin(
          LoginRequest(
            documentType: _documentType,
            documentNumber: docToSend,
            pin: _pinController.text,
          ),
        );

    if (!mounted) return;
    setState(() => _isLoading = false);

    final state = ref.read(authStateProvider);
    if (state.hasError) {
      _showFriendlyErrorFromErrorObject(state.error!);
      return;
    }
    if (state.hasValue && state.value != null) {
      context.go('/home');
    }
  }

  Future<void> _loginWithBiometric() async {
    if (_isWeb) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Biometría no disponible en la versión web.'),
          backgroundColor: Colors.orange,
          behavior: SnackBarBehavior.floating,
        ),
      );
      return;
    }

    try {
      final authenticated = await auth.authenticate(
        localizedReason: 'Por favor, autentícate para ingresar a Met',
        options: const AuthenticationOptions(
          biometricOnly: true,
          stickyAuth: true,
        ),
      );

      if (!authenticated) return;

      final token = await _storage.read(key: 'biometric_token');
      if (token == null) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
                'No hay huella vinculada a tu cuenta. Inicia con PIN primero.'),
            backgroundColor: Colors.orange,
            behavior: SnackBarBehavior.floating,
          ),
        );
        return;
      }

      setState(() => _isLoading = true);

      String docToSend = _documentController.text.trim();
      if (docToSend.contains('*') && _rawSavedDocument != null) {
        docToSend = _rawSavedDocument!;
      }

      await ref.read(authStateProvider.notifier).loginWithBiometric(
            LoginRequest(
              documentType: _documentType,
              documentNumber: docToSend,
              biometricPayload: token,
            ),
          );

      if (!mounted) return;
      setState(() => _isLoading = false);

      final state = ref.read(authStateProvider);
      if (state.hasError) {
        _showFriendlyErrorFromErrorObject(state.error!);
        return;
      }
      if (state.hasValue && state.value != null) {
        context.go('/home');
      }
    } on PlatformException catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error biométrico: ${e.message}'),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  Widget _buildPinInput() {
    return AnimatedBuilder(
      animation: _shakeAnimation,
      builder: (context, child) {
        final offset = math.sin(_shakeAnimation.value * math.pi) * 8;
        return Transform.translate(
          offset: Offset(offset, 0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: List.generate(4, (index) {
              return Padding(
                padding: const EdgeInsets.symmetric(horizontal: 12),
                child: _PinIndicator(
                  isFilled: index < _pinController.text.length,
                  isError: _isPinError,
                ),
              );
            }),
          ),
        );
      },
    );
  }

  Widget _buildNumericKeyboard() {
    return Column(
      children: [
        for (var i = 0; i < 3; i++)
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              for (var j = 1; j <= 3; j++)
                Expanded(
                  child: _NumericKey(
                    text: '${i * 3 + j}',
                    onPressed: () => _onKeypadPressed('${i * 3 + j}'),
                  ),
                ),
            ],
          ),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            Expanded(
              child: _canCheckBiometrics
                  ? _BiometricButton(onPressed: _loginWithBiometric)
                  : const SizedBox.shrink(),
            ),
            Expanded(
              child: _NumericKey(
                text: '0',
                onPressed: () => _onKeypadPressed('0'),
              ),
            ),
            Expanded(
              child: _NumericKey(
                icon: Icons.backspace_outlined,
                onPressed: _onKeypadBackspace,
              ),
            ),
          ],
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          // MEJORA 6 — Background Cinemático
          Container(
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                colors: [Color(0xFF1E1E1E), Color(0xFF121212)],
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
              ),
            ),
          ),
          Positioned(
            top: -100,
            left: -50,
            child: Container(
              width: 300,
              height: 300,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    const Color(0xFFE65100).withOpacity(0.4),
                    Colors.transparent,
                  ],
                ),
              ),
            ),
          ),
          Positioned(
            bottom: -50,
            right: -100,
            child: Container(
              width: 400,
              height: 400,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    const Color(0xFFFF9800).withOpacity(0.3),
                    Colors.transparent,
                  ],
                ),
              ),
            ),
          ),
          BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 30, sigmaY: 30),
            child: Container(color: Colors.transparent),
          ),
          // Contenido principal
          SafeArea(
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
                child: Form(
                  key: _formKey,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // MEJORA 2: Logo premium Met
                      _AnimatedEntrance(
                        delayMs: 0,
                        child: Container(
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            boxShadow: [
                              BoxShadow(
                                color: const Color(0xFFFF9800).withOpacity(0.4),
                                blurRadius: 30,
                                spreadRadius: 5,
                              )
                            ],
                          ),
                          child: ClipOval(
                            child: BackdropFilter(
                              filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
                              child: Container(
                                padding: const EdgeInsets.all(20),
                                decoration: BoxDecoration(
                                  shape: BoxShape.circle,
                                  color: Colors.white.withOpacity(0.1),
                                  border: Border.all(
                                      color: Colors.white.withOpacity(0.3), width: 1.5),
                                ),
                                child: const Text(
                                  'M',
                                  style: TextStyle(
                                    fontSize: 54,
                                    fontWeight: FontWeight.w900,
                                    color: Colors.white,
                                    letterSpacing: -2,
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 24),
                      // Título
                      _AnimatedEntrance(
                        delayMs: 100,
                        child: Column(
                          children: [
                            const Text(
                              'Bienvenido',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                fontSize: 32,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                              ),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              'Ingresa a tu cuenta para continuar',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                fontSize: 16,
                                color: Colors.white.withOpacity(0.8),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 32),
                      // Formulario
                      _AnimatedEntrance(
                        delayMs: 200,
                        child: _GlassCard(
                          padding: const EdgeInsets.all(24),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              Container(
                                padding: const EdgeInsets.all(4),
                                decoration: BoxDecoration(
                                  color: Colors.white.withOpacity(0.05),
                                  borderRadius: BorderRadius.circular(16),
                                  border: Border.all(color: Colors.white.withOpacity(0.1)),
                                ),
                                child: Row(
                                  children: [
                                    Expanded(
                                      child: GestureDetector(
                                        onTap: () => setState(() => _documentType = 'CC'),
                                        child: AnimatedContainer(
                                          duration: const Duration(milliseconds: 200),
                                          padding: const EdgeInsets.symmetric(vertical: 12),
                                          decoration: BoxDecoration(
                                            color: _documentType == 'CC' ? const Color(0xFFFF9800) : Colors.transparent,
                                            borderRadius: BorderRadius.circular(12),
                                          ),
                                          alignment: Alignment.center,
                                          child: Text('Cédula (CC)', style: TextStyle(color: _documentType == 'CC' ? Colors.white : Colors.white70, fontWeight: FontWeight.bold)),
                                        ),
                                      ),
                                    ),
                                    Expanded(
                                      child: GestureDetector(
                                        onTap: () => setState(() => _documentType = 'CE'),
                                        child: AnimatedContainer(
                                          duration: const Duration(milliseconds: 200),
                                          padding: const EdgeInsets.symmetric(vertical: 12),
                                          decoration: BoxDecoration(
                                            color: _documentType == 'CE' ? const Color(0xFFFF9800) : Colors.transparent,
                                            borderRadius: BorderRadius.circular(12),
                                          ),
                                          alignment: Alignment.center,
                                          child: Text('Extranjería (CE)', style: TextStyle(color: _documentType == 'CE' ? Colors.white : Colors.white70, fontWeight: FontWeight.bold)),
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(height: 16),
                              TextFormField(
                                controller: _documentController,
                                style: const TextStyle(color: Colors.white),
                                decoration: InputDecoration(
                                  labelText: 'Número de documento',
                                  labelStyle: TextStyle(
                                      color: Colors.white.withOpacity(0.8)),
                                  filled: true,
                                  fillColor: Colors.white.withOpacity(0.05),
                                  border: OutlineInputBorder(
                                    borderRadius: BorderRadius.circular(16),
                                    borderSide: BorderSide.none,
                                  ),
                                  focusedBorder: OutlineInputBorder(
                                    borderRadius: BorderRadius.circular(16),
                                    borderSide: const BorderSide(color: Color(0xFFFF9800), width: 1.5),
                                  ),
                                  prefixIcon: const Icon(Icons.badge_outlined, color: Colors.white70),
                                ),
                                keyboardType: TextInputType.number,
                                validator: (value) =>
                                    value == null || value.isEmpty
                                        ? 'Campo requerido'
                                        : null,
                                onChanged: (val) => _rawSavedDocument = null,
                              ),
                              const SizedBox(height: 16),
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  const Text('Recordarme',
                                      style: TextStyle(color: Colors.white, fontSize: 16)),
                                  Switch(
                                    value: _rememberMe,
                                    activeColor: const Color(0xFFFF9800),
                                    inactiveTrackColor: Colors.white.withOpacity(0.1),
                                    onChanged: (value) => setState(
                                        () => _rememberMe = value),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 24),
                      // PIN y Teclado
                      _AnimatedEntrance(
                        delayMs: 300,
                        child: Column(
                          children: [
                            Text(
                              'PIN de acceso',
                              style: TextStyle(
                                color: Colors.white.withOpacity(0.8),
                                fontSize: 16,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                            const SizedBox(height: 16),
                            _buildPinInput(),
                            const SizedBox(height: 8),
                            Align(
                              alignment: Alignment.centerRight,
                              child: TextButton(
                                onPressed: () {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(
                                      content: Text(
                                          'Flujo de recuperación de PIN en construcción'),
                                      behavior: SnackBarBehavior.floating,
                                    ),
                                  );
                                },
                                style: TextButton.styleFrom(
                                  foregroundColor: Colors.white.withOpacity(0.9),
                                  padding: EdgeInsets.zero,
                                  minimumSize: const Size(50, 30),
                                  tapTargetSize:
                                      MaterialTapTargetSize.shrinkWrap,
                                ),
                                child: const Text(
                                  '¿Olvidaste tu PIN?',
                                  style: TextStyle(
                                      decoration: TextDecoration.underline,
                                      fontSize: 14),
                                ),
                              ),
                            ),
                            const SizedBox(height: 16),
                            // Teclado
                            _buildNumericKeyboard(),
                            const SizedBox(height: 24),
                            // Botón Ingresar
                            ElevatedButton(
                              style: ElevatedButton.styleFrom(
                                backgroundColor: const Color(0xFFFF9800),
                                foregroundColor: Colors.white,
                                padding:
                                    const EdgeInsets.symmetric(vertical: 16),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(16),
                                ),
                                elevation: 5,
                                shadowColor:
                                    const Color(0xFFFF9800).withOpacity(0.5),
                              ),
                              onPressed: _isLoading ? null : _loginWithPin,
                              child: _isLoading
                                  ? const SizedBox(
                                      width: 24,
                                      height: 24,
                                      child: CircularProgressIndicator(
                                          strokeWidth: 2, color: Colors.white),
                                    )
                                  : const Text(
                                      'Ingresar',
                                      style: TextStyle(
                                          fontSize: 18,
                                          fontWeight: FontWeight.bold),
                                    ),
                            ),
                            const SizedBox(height: 16),
                            TextButton(
                              onPressed: () => context.push('/register'),
                              style: TextButton.styleFrom(
                                  foregroundColor: Colors.white),
                              child:
                                  const Text('¿No tienes cuenta? Regístrate'),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
