import 'dart:ui';
import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:dio/dio.dart';
import 'package:go_router/go_router.dart';
import 'package:local_auth/local_auth.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../data/models/auth_models.dart';
import '../providers/auth_provider.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../../core/security/behavioral_biometrics_service.dart';
import '../../../../core/session/session_expired_provider.dart';

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
            color: Theme.of(context).colorScheme.onSurface.withOpacity(0.05),
            borderRadius: BorderRadius.circular(borderRadius),
            border: Border.all(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.1)),
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
      color = Theme.of(context).colorScheme.primary;
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
              : (isFilled ? Theme.of(context).colorScheme.primary : Theme.of(context).colorScheme.onSurface.withOpacity(0.3)),
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
            color: Theme.of(context).colorScheme.onSurface.withOpacity(0.05),
            border: Border.all(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.1)),
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
                  ? Icon(widget.icon, color: Theme.of(context).colorScheme.onSurface, size: 28)
                  : Text(
                      widget.text,
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.onSurface,
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
            color: Theme.of(context).colorScheme.onSurface.withOpacity(_isHovered ? 0.1 : 0.05),
            border: Border.all(
              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.2),
              width: 1.5,
            ),
            boxShadow: [
              BoxShadow(
                color: Theme.of(context).colorScheme.primary.withOpacity(_isHovered ? 0.5 : 0.2),
                blurRadius: _isHovered ? 20 : 10,
                spreadRadius: _isHovered ? 5 : 2,
              )
            ],
          ),
          child: AspectRatio(
            aspectRatio: 1,
            child: Center(
              child: Icon(
                Icons.fingerprint,
                color: Theme.of(context).colorScheme.onSurface,
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

    // Iniciar telemetría conductual
    BehavioralBiometricsService().startSession();

    // Si el interceptor de red nos trajo aquí por un 401 (sesión inválida/expirada),
    // avisamos al usuario en vez de dejarlo adivinar por qué volvió al login.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      if (ref.read(sessionExpiredProvider)) {
        ref.read(sessionExpiredProvider.notifier).state = false;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Tu sesión expiró. Ingresa de nuevo con tu PIN.'),
            duration: Duration(seconds: 4),
          ),
        );
      }
    });
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
      // Registrar cadencia de tecleo
      BehavioralBiometricsService().recordKeystroke();
      
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
    BehavioralBiometricsService().stopSession();
    _documentController.dispose();
    _pinController.dispose();
    _shakeController.dispose();
    super.dispose();
  }

  /// Lee el código y mensaje de negocio reales que manda el backend.
  ///
  /// IMPORTANTE: `DioException.toString()` NUNCA incluye el cuerpo JSON de la
  /// respuesta (solo una plantilla genérica tipo "the response has a status
  /// code of 422..."). Por eso hay que leer `error.response?.data` directamente
  /// en vez de comparar texto contra `error.toString()`.
  (String? code, String? message) _extractServerError(Object error) {
    if (error is DioException) {
      final data = error.response?.data;
      if (data is Map) {
        return (data['code'] as String?, data['message'] as String?);
      }
    }
    return (null, null);
  }

  void _showFriendlyErrorFromErrorObject(Object error) {
    if (error is String && error.startsWith('FRAUD_DETECTED')) {
      _triggerError();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Detectamos un comportamiento inusual. Por tu seguridad, bloqueamos este intento.'),
          backgroundColor: Colors.redAccent,
          behavior: SnackBarBehavior.floating,
        ),
      );
      return;
    }

    final (code, serverMessage) = _extractServerError(error);

    if (code == 'DEVICE_NOT_RECOGNIZED') {
      _promptDeviceOtp();
      return;
    }
    if (code == 'INVALID_OTP') {
      _promptDeviceOtp(errorText: serverMessage ?? 'El código es incorrecto o expiró. Intenta de nuevo.');
      return;
    }

    String friendlyMessage;
    switch (code) {
      case 'USER_NOT_ACTIVE':
        friendlyMessage = 'Debes verificar tu correo antes de iniciar sesión. Revisa tu bandeja de entrada.';
        final docType = _documentType;
        final docNumber = _documentController.text.trim();
        if (docNumber.isNotEmpty) {
          Future.delayed(const Duration(seconds: 2), () {
            if (mounted) context.push('/verify-email', extra: {'documentType': docType, 'documentNumber': docNumber});
          });
        }
        break;
      case 'ACCOUNT_LOCKED':
        friendlyMessage = serverMessage ?? 'Tu cuenta ha sido bloqueada temporalmente por demasiados intentos fallidos. Revisa tu correo.';
        break;
      case 'INVALID_CREDENTIALS':
        friendlyMessage = serverMessage ?? 'El PIN es incorrecto. Por favor, verifica e intenta de nuevo.';
        _triggerError();
        break;
      case 'RATE_LIMIT_EXCEEDED':
        friendlyMessage = serverMessage ?? 'Demasiados intentos. Espera unos segundos e inténtalo de nuevo.';
        break;
      default:
        friendlyMessage = serverMessage ?? 'Ocurrió un error inesperado. Por favor, intenta de nuevo.';
        _triggerError();
    }

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(friendlyMessage),
        backgroundColor: Colors.redAccent,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  Future<void> _loginWithPin({String? otpCode}) async {
    if (!_formKey.currentState!.validate()) return;
    if (_pinController.text.length != 4) {
       _triggerError();
       return;
    }

    // Análisis de Biometría Conductual local antes de enviar
    if (otpCode == null && BehavioralBiometricsService().analyzeAndDetectFraud()) {
       _showFriendlyErrorFromErrorObject('FRAUD_DETECTED: Patrón de comportamiento anómalo detectado. Por su seguridad, el acceso ha sido bloqueado.');
       // Reiniciar la sesión por si fue un falso positivo y quiere reintentar
       BehavioralBiometricsService().startSession();
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
            otpCode: otpCode,
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
      if (state.value!.kycStatus == 'PENDING') {
        context.go('/biometric-registration');
      } else {
        context.go('/home');
      }
    }
  }

  /// Se muestra cuando el backend detecta un dispositivo/navegador no
  /// reconocido y envía un código de seguridad al correo del usuario.
  Future<void> _promptDeviceOtp({String? errorText}) async {
    final otpController = TextEditingController();
    String? dialogError = errorText;

    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: const Text('Nuevo dispositivo detectado'),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Por seguridad, enviamos un código a tu correo. Ingrésalo para continuar.'),
                  const SizedBox(height: 16),
                  TextField(
                    controller: otpController,
                    keyboardType: TextInputType.number,
                    maxLength: 6,
                    autofocus: true,
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 22, letterSpacing: 8),
                    decoration: const InputDecoration(hintText: '------', counterText: ''),
                  ),
                  if (dialogError != null) ...[
                    const SizedBox(height: 8),
                    Text(dialogError!, style: const TextStyle(color: Colors.red)),
                  ],
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(dialogContext).pop(),
                  child: const Text('Cancelar'),
                ),
                FilledButton(
                  onPressed: () {
                    final code = otpController.text.trim();
                    if (code.length != 6) {
                      setDialogState(() => dialogError = 'Ingresa el código de 6 dígitos.');
                      return;
                    }
                    Navigator.of(dialogContext).pop();
                    _loginWithPin(otpCode: code);
                  },
                  child: const Text('Verificar'),
                ),
              ],
            );
          },
        );
      },
    );
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
        if (state.value!.kycStatus == 'PENDING') {
          context.go('/biometric-registration');
        } else {
          context.go('/home');
        }
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
    // El login SIEMPRE muestra el tema oscuro, sin importar el modo seleccionado
    return Theme(
      data: AppTheme.darkTheme,
      child: Builder(
        builder: (context) => Scaffold(
      body: Listener(
        onPointerDown: (event) => BehavioralBiometricsService().recordPointerEvent(event),
        onPointerMove: (event) => BehavioralBiometricsService().recordPointerEvent(event),
        child: Stack(
          children: [
          // MEJORA 6 — Background Cinemático (siempre oscuro)
          Container(
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                colors: [Color(0xFF637C5A), Color(0xFF415739)],
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
                    Colors.orange.withOpacity(0.4),
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
                    Theme.of(context).colorScheme.primary.withOpacity(0.3),
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
                      // Logo Met — estilo app icon
                      _AnimatedEntrance(
                        delayMs: 0,
                        child: Center(
                          child: Container(
                            width: 140,
                            height: 140,
                            decoration: BoxDecoration(
                              color: Colors.transparent,
                              borderRadius: BorderRadius.circular(32),
                              boxShadow: [
                                BoxShadow(
                                  color: Colors.black.withOpacity(0.15),
                                  blurRadius: 30,
                                  spreadRadius: 2,
                                  offset: const Offset(0, 12),
                                ),
                              ],
                            ),
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(32),
                              child: Image.asset(
                                'assets/images/logo.png',
                                fit: BoxFit.contain,
                                errorBuilder: (context, error, stackTrace) =>
                                    const Icon(
                                  Icons.image_not_supported,
                                  color: Colors.white54,
                                  size: 40,
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                      SizedBox(height: 24),
                      // Título
                      _AnimatedEntrance(
                        delayMs: 100,
                        child: Column(
                          children: [
                            Text(
                              'Bienvenido',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                fontSize: 32,
                                fontWeight: FontWeight.bold,
                                color: Theme.of(context).colorScheme.onSurface,
                              ),
                            ),
                            SizedBox(height: 8),
                            Text(
                              'Ingresa a tu cuenta para continuar',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                fontSize: 16,
                                color: Theme.of(context).colorScheme.onSurface.withOpacity(0.8),
                              ),
                            ),
                          ],
                        ),
                      ),
                      SizedBox(height: 32),
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
                                  color: Theme.of(context).colorScheme.onSurface.withOpacity(0.05),
                                  borderRadius: BorderRadius.circular(16),
                                  border: Border.all(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.1)),
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
                                            color: _documentType == 'CC' ? Theme.of(context).colorScheme.primary : Colors.transparent,
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
                                            color: _documentType == 'CE' ? Theme.of(context).colorScheme.primary : Colors.transparent,
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
                              SizedBox(height: 16),
                              TextFormField(
                                controller: _documentController,
                                style: TextStyle(color: Theme.of(context).colorScheme.onSurface),
                                decoration: InputDecoration(
                                  labelText: 'Número de documento',
                                  labelStyle: TextStyle(
                                      color: Theme.of(context).colorScheme.onSurface.withOpacity(0.8)),
                                  filled: true,
                                  fillColor: Colors.white.withOpacity(0.05),
                                  border: OutlineInputBorder(
                                    borderRadius: BorderRadius.circular(16),
                                    borderSide: BorderSide.none,
                                  ),
                                  focusedBorder: OutlineInputBorder(
                                    borderRadius: BorderRadius.circular(16),
                                    borderSide: const BorderSide(color: Color(0xFF53A835), width: 1.5),
                                  ),
                                  prefixIcon: Icon(Icons.badge_outlined, color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)),
                                ),
                                keyboardType: TextInputType.number,
                                validator: (value) =>
                                    value == null || value.isEmpty
                                        ? 'Campo requerido'
                                        : null,
                                onChanged: (val) => _rawSavedDocument = null,
                              ),
                              SizedBox(height: 16),
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Text('Recordarme',
                                      style: TextStyle(color: Theme.of(context).colorScheme.onSurface, fontSize: 16)),
                                  Switch(
                                    value: _rememberMe,
                                    activeColor: Theme.of(context).colorScheme.primary,
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
                      SizedBox(height: 24),
                      // PIN y Teclado
                      _AnimatedEntrance(
                        delayMs: 300,
                        child: Column(
                          children: [
                            Text(
                              'PIN de acceso',
                              style: TextStyle(
                                color: Theme.of(context).colorScheme.onSurface.withOpacity(0.8),
                                fontSize: 16,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                            SizedBox(height: 16),
                            _buildPinInput(),
                            const SizedBox(height: 8),
                            Align(
                              alignment: Alignment.centerRight,
                              child: TextButton(
                                onPressed: () {
                                  context.push('/auth/recover-pin');
                                },
                                style: TextButton.styleFrom(
                                  foregroundColor: Colors.white.withOpacity(0.9),
                                  padding: EdgeInsets.zero,
                                  minimumSize: const Size(50, 30),
                                  tapTargetSize:
                                      MaterialTapTargetSize.shrinkWrap,
                                ),
                                child: Text(
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
                                backgroundColor: Theme.of(context).colorScheme.primary,
                                foregroundColor: Colors.white,
                                padding:
                                    const EdgeInsets.symmetric(vertical: 16),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(16),
                                ),
                                elevation: 5,
                                shadowColor:
                                    Theme.of(context).colorScheme.primary.withOpacity(0.5),
                              ),
                              onPressed: _isLoading ? null : _loginWithPin,
                              child: _isLoading
                                  ? SizedBox(
                                      width: 24,
                                      height: 24,
                                      child: CircularProgressIndicator(
                                          strokeWidth: 2, color: Theme.of(context).colorScheme.onSurface),
                                    )
                                  : Text(
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
                                  Text('¿No tienes cuenta? Regístrate'),
                            ),
                            // Acceso admin removido
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
      ),
      ),
      ),
    );
  }
}
