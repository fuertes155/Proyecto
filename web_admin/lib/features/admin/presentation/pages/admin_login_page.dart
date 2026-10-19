import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../providers/admin_provider.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../../core/ui/components.dart' show MetLogo;

/// Inicio de sesión del panel administrativo web.
///
/// En pantallas anchas muestra dos columnas (marca + formulario); en angostas, solo el
/// formulario con la marca encima. Pensado para teclado: el usuario recibe el foco al
/// abrir, Enter avanza/envía, el navegador puede autocompletar las credenciales y se
/// avisa si Bloq Mayús está activado.
class AdminLoginPage extends ConsumerStatefulWidget {
  const AdminLoginPage({super.key});

  @override
  ConsumerState<AdminLoginPage> createState() => _AdminLoginPageState();
}

class _AdminLoginPageState extends ConsumerState<AdminLoginPage> {
  static const _wideBreakpoint = 960.0;

  final _formKey = GlobalKey<FormState>();
  final _usernameCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  final _passwordFocus = FocusNode();

  bool _obscurePassword = true;
  bool _isLoading = false;
  bool _capsLockOn = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    HardwareKeyboard.instance.addHandler(_onKeyEvent);
    _capsLockOn = _isCapsLockOn();
  }

  @override
  void dispose() {
    HardwareKeyboard.instance.removeHandler(_onKeyEvent);
    _usernameCtrl.dispose();
    _passwordCtrl.dispose();
    _passwordFocus.dispose();
    super.dispose();
  }

  bool _isCapsLockOn() =>
      HardwareKeyboard.instance.lockModesEnabled.contains(KeyboardLockMode.capsLock);

  bool _onKeyEvent(KeyEvent event) {
    final capsLock = _isCapsLockOn();
    if (capsLock != _capsLockOn && mounted) {
      setState(() => _capsLockOn = capsLock);
    }
    return false; // solo observamos, no consumimos la tecla
  }

  Future<void> _login() async {
    if (_isLoading) return;
    setState(() => _errorMessage = null);
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);
    await ref.read(adminAuthProvider.notifier).login(
          _usernameCtrl.text.trim(),
          _passwordCtrl.text,
        );
    if (!mounted) return;

    final state = ref.read(adminAuthProvider);
    if (state.hasError) {
      setState(() {
        _isLoading = false;
        _errorMessage = _friendlyError(state.error);
      });
      _passwordCtrl.clear();
      _passwordFocus.requestFocus();
      return;
    }

    setState(() => _isLoading = false);
    if (state.hasValue && state.value != null) {
      TextInput.finishAutofillContext(); // el navegador ofrece guardar la contraseña
      context.go('/admin/dashboard');
    }
  }

  String _friendlyError(Object? error) {
    if (error is String && error.isNotEmpty) {
      return error == 'Credenciales inválidas'
          ? 'Usuario o contraseña incorrectos. Revisa los datos e inténtalo de nuevo.'
          : error;
    }
    if (error is DioException) {
      switch (error.type) {
        case DioExceptionType.connectionError:
        case DioExceptionType.connectionTimeout:
        case DioExceptionType.receiveTimeout:
        case DioExceptionType.sendTimeout:
          return 'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.';
        default:
          if (error.response?.statusCode == 429) {
            return 'Demasiados intentos. Espera un minuto antes de volver a intentar.';
          }
      }
    }
    return 'No fue posible iniciar sesión. Inténtalo de nuevo en unos segundos.';
  }

  @override
  Widget build(BuildContext context) {
    // El panel siempre se muestra en modo claro.
    return Theme(
      data: AppTheme.lightTheme,
      child: Scaffold(
        backgroundColor: MetColors.canvas,
        body: LayoutBuilder(
          builder: (context, constraints) {
            final wide = constraints.maxWidth >= _wideBreakpoint;
            final form = _buildFormArea(showCompactBrand: !wide);
            if (!wide) return form;
            return Row(
              children: [
                const Expanded(flex: 5, child: _BrandPanel()),
                Expanded(flex: 6, child: form),
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _buildFormArea({required bool showCompactBrand}) {
    return SafeArea(
      child: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 32),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 440),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                if (showCompactBrand) ...[
                  const _CompactBrand(),
                  const SizedBox(height: 28),
                ],
                _buildCard(),
                const SizedBox(height: 20),
                _buildHelpFooter(),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildCard() {
    final scheme = AppTheme.lightTheme.colorScheme;
    return Container(
      padding: const EdgeInsets.fromLTRB(32, 36, 32, 32),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0xFFE3EAE3)),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF1B4D2B).withValues(alpha: 0.06),
            blurRadius: 32,
            offset: const Offset(0, 16),
          ),
        ],
      ),
      child: AutofillGroup(
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'Bienvenido de nuevo',
                style: TextStyle(
                  fontSize: 26,
                  fontWeight: FontWeight.w800,
                  color: Colors.grey.shade900,
                  letterSpacing: -0.4,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                'Inicia sesión con tu cuenta de administrador de MET.',
                style: TextStyle(fontSize: 15, color: Colors.grey.shade600, height: 1.4),
              ),
              const SizedBox(height: 28),

              if (_errorMessage != null) ...[
                _ErrorBanner(message: _errorMessage!),
                const SizedBox(height: 20),
              ],

              const _FieldLabel('Usuario administrador'),
              const SizedBox(height: 8),
              TextFormField(
                controller: _usernameCtrl,
                enabled: !_isLoading,
                autofocus: true,
                autofillHints: const [AutofillHints.username],
                textInputAction: TextInputAction.next,
                onFieldSubmitted: (_) => _passwordFocus.requestFocus(),
                onChanged: (_) {
                  if (_errorMessage != null) setState(() => _errorMessage = null);
                },
                decoration: _inputDecoration(
                  hint: 'Tu usuario, p. ej. admin.juan',
                  icon: Icons.person_outline_rounded,
                ),
                validator: (v) =>
                    v == null || v.trim().isEmpty ? 'Escribe tu usuario de administrador' : null,
              ),
              const SizedBox(height: 20),

              const _FieldLabel('Contraseña'),
              const SizedBox(height: 8),
              TextFormField(
                controller: _passwordCtrl,
                focusNode: _passwordFocus,
                enabled: !_isLoading,
                obscureText: _obscurePassword,
                autofillHints: const [AutofillHints.password],
                textInputAction: TextInputAction.done,
                onFieldSubmitted: (_) => _login(),
                onChanged: (_) {
                  if (_errorMessage != null) setState(() => _errorMessage = null);
                },
                decoration: _inputDecoration(
                  hint: 'Mínimo 8 caracteres',
                  icon: Icons.lock_outline_rounded,
                ).copyWith(
                  suffixIcon: Padding(
                    padding: const EdgeInsets.only(right: 4),
                    child: IconButton(
                      tooltip: _obscurePassword ? 'Mostrar contraseña' : 'Ocultar contraseña',
                      icon: Icon(
                        _obscurePassword ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                        color: Colors.grey.shade600,
                      ),
                      onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                    ),
                  ),
                ),
                validator: (v) {
                  if (v == null || v.isEmpty) return 'Escribe tu contraseña';
                  if (v.length < 8) return 'La contraseña tiene al menos 8 caracteres';
                  return null;
                },
              ),
              if (_capsLockOn) ...[
                const SizedBox(height: 10),
                Row(
                  children: [
                    Icon(Icons.keyboard_capslock_rounded, size: 18, color: Colors.amber.shade800),
                    const SizedBox(width: 6),
                    Flexible(
                      child: Text(
                        'Bloq Mayús está activado',
                        style: TextStyle(
                          fontSize: 13,
                          color: Colors.amber.shade900,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 28),

              SizedBox(
                height: 54,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _login,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: scheme.primary,
                    foregroundColor: Colors.white,
                    disabledBackgroundColor: scheme.primary.withValues(alpha: 0.75),
                    disabledForegroundColor: Colors.white,
                    elevation: 0,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                  ),
                  child: _isLoading
                      ? const Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                            ),
                            SizedBox(width: 12),
                            Text('Verificando…',
                                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
                          ],
                        )
                      : const Text('Iniciar sesión',
                          style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
                ),
              ),
              const SizedBox(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.lock_rounded, size: 14, color: Colors.grey.shade500),
                  const SizedBox(width: 6),
                  Flexible(
                    child: Text(
                      'Conexión cifrada · Sesión con registro de auditoría',
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 12.5, color: Colors.grey.shade600),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHelpFooter() {
    return Text(
      '¿Olvidaste tu contraseña o tu usuario está bloqueado?\n'
      'Pide a un superadministrador que restablezca tus credenciales.',
      textAlign: TextAlign.center,
      style: TextStyle(fontSize: 13, color: Colors.grey.shade600, height: 1.5),
    );
  }

  InputDecoration _inputDecoration({required String hint, required IconData icon}) {
    final primary = AppTheme.lightTheme.colorScheme.primary;
    OutlineInputBorder border(Color color, [double width = 1.2]) => OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: color, width: width),
        );
    return InputDecoration(
      hintText: hint,
      hintStyle: TextStyle(color: Colors.grey.shade400, fontSize: 15),
      prefixIcon: Icon(icon, color: Colors.grey.shade500, size: 22),
      filled: true,
      fillColor: const Color(0xFFF8FAF8),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
      border: border(const Color(0xFFDDE5DD)),
      enabledBorder: border(const Color(0xFFDDE5DD)),
      disabledBorder: border(const Color(0xFFE8ECE8)),
      focusedBorder: border(primary, 2),
      errorBorder: border(Colors.red.shade300),
      focusedErrorBorder: border(Colors.red.shade400, 2),
      errorStyle: const TextStyle(fontSize: 13),
    );
  }
}

class _FieldLabel extends StatelessWidget {
  const _FieldLabel(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: Colors.grey.shade800),
    );
  }
}

class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      liveRegion: true,
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: const Color(0xFFFDECEC),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: const Color(0xFFF5C2C2)),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(Icons.error_outline_rounded, color: Colors.red.shade700, size: 20),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                message,
                style: TextStyle(color: Colors.red.shade900, fontSize: 14, height: 1.4),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Marca encima del formulario en pantallas angostas.
class _CompactBrand extends StatelessWidget {
  const _CompactBrand();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        const MetLogo(size: 64),
        const SizedBox(height: 14),
        Text(
          'MET Admin',
          style: TextStyle(
            fontSize: 22,
            fontWeight: FontWeight.w800,
            color: Colors.grey.shade900,
            letterSpacing: -0.3,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          'Panel administrativo de la cooperativa',
          style: TextStyle(fontSize: 14, color: Colors.grey.shade600),
        ),
        const SizedBox(height: 10),
        const Text(
          kMetSlogan,
          textAlign: TextAlign.center,
          style: TextStyle(
            fontSize: 13.5,
            fontStyle: FontStyle.italic,
            fontWeight: FontWeight.w600,
            color: MetColors.primary,
          ),
        ),
      ],
    );
  }
}

/// Columna de marca en pantallas anchas.
class _BrandPanel extends StatelessWidget {
  const _BrandPanel();

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        gradient: MetColors.brandGradient,
      ),
      child: Stack(
        children: [
          Positioned(top: -120, right: -120, child: _glow(360, 0.10)),
          Positioned(bottom: -160, left: -100, child: _glow(420, 0.08)),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(56, 48, 56, 40),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(
                    children: [
                      MetLogo(size: 48),
                      SizedBox(width: 14),
                      Text(
                        'MET Admin',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 22,
                          fontWeight: FontWeight.w800,
                          letterSpacing: -0.3,
                        ),
                      ),
                    ],
                  ),
                  const Spacer(),
                  const Text(
                    'Impulso instantáneo,\nconfianza permanente.',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 34,
                      height: 1.2,
                      fontWeight: FontWeight.w800,
                      letterSpacing: -0.6,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'El panel de MET para gestionar socios, cuentas, créditos, cumplimiento y auditoría en un solo lugar.',
                    style: TextStyle(
                      color: Colors.white.withValues(alpha: 0.8),
                      fontSize: 16,
                      height: 1.5,
                    ),
                  ),
                  const SizedBox(height: 36),
                  const _BrandPoint(
                    icon: Icons.verified_user_outlined,
                    text: 'Acceso exclusivo para personal autorizado',
                  ),
                  const _BrandPoint(
                    icon: Icons.history_rounded,
                    text: 'Cada acción queda registrada en la auditoría',
                  ),
                  const _BrandPoint(
                    icon: Icons.timer_outlined,
                    text: 'Tu sesión vence automáticamente por seguridad',
                  ),
                  const Spacer(),
                  Text(
                    '© ${DateTime.now().year} MET · Cooperativa',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.55), fontSize: 13),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  static Widget _glow(double size, double alpha) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: RadialGradient(
          colors: [const Color(0xFF8BC34A).withValues(alpha: alpha), Colors.transparent],
        ),
      ),
    );
  }
}

class _BrandPoint extends StatelessWidget {
  const _BrandPoint({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 14),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, color: Colors.white, size: 18),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Text(
              text,
              style: TextStyle(color: Colors.white.withValues(alpha: 0.9), fontSize: 15),
            ),
          ),
        ],
      ),
    );
  }
}
