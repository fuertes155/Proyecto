import 'dart:ui';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pinput/pinput.dart';
import 'package:local_auth/local_auth.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../../../core/widgets/accessible_button.dart';
import '../../../../core/widgets/custom_numeric_keypad.dart';
import '../../data/models/auth_models.dart';
import '../providers/auth_provider.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
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

  bool get _isWeb => kIsWeb;

  @override
  void initState() {
    super.initState();
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
      _pinController.text += value;
    }
  }

  void _onKeypadBackspace() {
    if (_pinController.text.isNotEmpty) {
      _pinController.text =
          _pinController.text.substring(0, _pinController.text.length - 1);
    }
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
    } else if (lowerError.contains('not found') ||
        lowerError.contains('usuario')) {
      friendlyMessage = 'No encontramos un usuario con este documento.';
    } else if (lowerError.contains('401') ||
        lowerError.contains('unauthorized')) {
      friendlyMessage =
          'No autorizado. Verifica tus credenciales e inténtalo de nuevo.';
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

  @override
  Widget build(BuildContext context) {
    final defaultPinTheme = PinTheme(
      width: 56,
      height: 56,
      textStyle: const TextStyle(
          fontSize: 24, color: Colors.white, fontWeight: FontWeight.bold),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.2),
        border: Border.all(color: Colors.white.withOpacity(0.5)),
        borderRadius: BorderRadius.circular(12),
      ),
    );

    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Color(0xFFE65100), Color(0xFFFF9800)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
        ),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(24),
                child: BackdropFilter(
                  filter: ImageFilter.blur(sigmaX: 15, sigmaY: 15),
                  child: Container(
                    padding: const EdgeInsets.all(32),
                    decoration: BoxDecoration(
                      color: Colors.white.withOpacity(0.15),
                      borderRadius: BorderRadius.circular(24),
                      border: Border.all(color: Colors.white.withOpacity(0.3)),
                    ),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          const Icon(Icons.account_balance_wallet,
                              size: 64, color: Colors.white),
                          const SizedBox(height: 16),
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
                              color: Colors.white.withOpacity(0.9),
                            ),
                          ),
                          const SizedBox(height: 32),
                          Theme(
                            data: Theme.of(context).copyWith(
                              canvasColor: const Color(0xFFE65100),
                            ),
                            child: DropdownButtonFormField<String>(
                              value: _documentType,
                              dropdownColor: const Color(0xFFE65100),
                              iconEnabledColor: Colors.white,
                              style: const TextStyle(
                                  color: Colors.white, fontSize: 16),
                              decoration: InputDecoration(
                                labelText: 'Tipo de documento',
                                labelStyle: TextStyle(
                                    color: Colors.white.withOpacity(0.8)),
                                enabledBorder: UnderlineInputBorder(
                                  borderSide: BorderSide(
                                      color: Colors.white.withOpacity(0.5)),
                                ),
                                focusedBorder: const UnderlineInputBorder(
                                  borderSide: BorderSide(color: Colors.white),
                                ),
                              ),
                              items: const [
                                DropdownMenuItem(
                                    value: 'CC',
                                    child: Text('Cédula de ciudadanía')),
                                DropdownMenuItem(
                                    value: 'CE',
                                    child: Text('Cédula de extranjería')),
                              ],
                              onChanged: (value) =>
                                  setState(() => _documentType = value ?? 'CC'),
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
                              enabledBorder: UnderlineInputBorder(
                                borderSide: BorderSide(
                                    color: Colors.white.withOpacity(0.5)),
                              ),
                              focusedBorder: const UnderlineInputBorder(
                                borderSide: BorderSide(color: Colors.white),
                              ),
                            ),
                            keyboardType: TextInputType.number,
                            validator: (value) => value == null || value.isEmpty
                                ? 'Campo requerido'
                                : null,
                            onChanged: (val) => _rawSavedDocument = null,
                          ),
                          const SizedBox(height: 8),
                          Row(
                            children: [
                              Theme(
                                data: Theme.of(context).copyWith(
                                  unselectedWidgetColor: Colors.white70,
                                ),
                                child: Checkbox(
                                  value: _rememberMe,
                                  activeColor: Colors.white,
                                  checkColor: const Color(0xFFE65100),
                                  onChanged: (value) => setState(
                                      () => _rememberMe = value ?? false),
                                ),
                              ),
                              const Text('Recordarme',
                                  style: TextStyle(color: Colors.white)),
                            ],
                          ),
                          const SizedBox(height: 16),
                          Text(
                            'PIN de acceso',
                            style: TextStyle(
                                color: Colors.white.withOpacity(0.8),
                                fontSize: 14),
                          ),
                          const SizedBox(height: 8),
                          Pinput(
                            controller: _pinController,
                            length: 4,
                            obscureText: true,
                            useNativeKeyboard: false,
                            keyboardType: TextInputType.number,
                            defaultPinTheme: defaultPinTheme,
                            focusedPinTheme: defaultPinTheme.copyWith(
                              decoration: defaultPinTheme.decoration!.copyWith(
                                border: Border.all(color: Colors.white),
                              ),
                            ),
                            validator: (value) {
                              if (value == null || value.length != 4) {
                                return 'El PIN debe tener 4 dígitos';
                              }
                              return null;
                            },
                          ),
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
                                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                              ),
                              child: const Text(
                                '¿Olvidaste tu PIN?',
                                style: TextStyle(
                                    decoration: TextDecoration.underline,
                                    fontSize: 14),
                              ),
                            ),
                          ),
                          const SizedBox(height: 24),
                          CustomNumericKeypad(
                            onKeyPressed: _onKeypadPressed,
                            onBackspace: _onKeypadBackspace,
                            showBiometric: _canCheckBiometrics,
                            onBiometric: _loginWithBiometric,
                          ),
                          const SizedBox(height: 32),
                          ElevatedButton(
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.white,
                              foregroundColor: const Color(0xFFE65100),
                              padding: const EdgeInsets.symmetric(vertical: 16),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                              elevation: 0,
                            ),
                            onPressed: _isLoading ? null : _loginWithPin,
                            child: _isLoading
                                ? const SizedBox(
                                    width: 24,
                                    height: 24,
                                    child: CircularProgressIndicator(
                                        strokeWidth: 2),
                                  )
                                : const Text(
                                    'Ingresar',
                                    style: TextStyle(
                                        fontSize: 18,
                                        fontWeight: FontWeight.bold),
                                  ),
                          ),
                          const SizedBox(height: 24),
                          TextButton(
                            onPressed: () => context.push('/register'),
                            style: TextButton.styleFrom(
                                foregroundColor: Colors.white),
                            child: const Text('¿No tienes cuenta? Regístrate'),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
