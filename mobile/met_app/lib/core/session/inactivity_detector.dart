import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:local_auth/local_auth.dart';
import '../storage/secure_storage_service.dart';
import '../../features/auth/presentation/providers/auth_provider.dart';

/// Widget que detecta inactividad del usuario y cierra la sesión automáticamente
/// si el usuario lleva más de [_timeoutDuration] sin interactuar con la app.
class InactivityDetector extends ConsumerStatefulWidget {
  final Widget child;

  const InactivityDetector({super.key, required this.child});

  @override
  ConsumerState<InactivityDetector> createState() => _InactivityDetectorState();
}

class _InactivityDetectorState extends ConsumerState<InactivityDetector>
    with WidgetsBindingObserver {
  static const _timeoutDuration = Duration(minutes: 5);
  static const _backgroundLockDuration = Duration(seconds: 10);

  Timer? _inactivityTimer;
  DateTime? _pausedAt;
  bool _isAuthenticating = false;
  final LocalAuthentication _localAuth = LocalAuthentication();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _resetTimer();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _inactivityTimer?.cancel();
    super.dispose();
  }

  /// Se llama cuando la app vuelve al primer plano (foreground)
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      if (_pausedAt != null) {
        final backgroundDuration = DateTime.now().difference(_pausedAt!);
        if (backgroundDuration > _backgroundLockDuration) {
          _requireBiometrics();
        }
      }
      _pausedAt = null;
      _checkIfSessionExpired();
    } else if (state == AppLifecycleState.paused) {
      _pausedAt = DateTime.now();
      _saveLastActivity();
      _inactivityTimer?.cancel();
    }
  }

  Future<void> _requireBiometrics() async {
    final authState = ref.read(authStateProvider);
    if (authState.valueOrNull == null || _isAuthenticating) return;

    _isAuthenticating = true;
    try {
      final canCheckBiometrics = await _localAuth.canCheckBiometrics;
      final isDeviceSupported = await _localAuth.isDeviceSupported();

      if (canCheckBiometrics || isDeviceSupported) {
        final authenticated = await _localAuth.authenticate(
          localizedReason: 'Autentícate para reanudar la sesión',
          options: const AuthenticationOptions(
            stickyAuth: true,
            biometricOnly: false,
          ),
        );
        if (!authenticated) {
          _expireSession();
        }
      }
    } catch (e) {
      // Si ocurre un error, por seguridad cerramos sesión
      _expireSession();
    } finally {
      _isAuthenticating = false;
    }
  }

  void _resetTimer() {
    _inactivityTimer?.cancel();
    _saveLastActivity();
    _inactivityTimer = Timer(_timeoutDuration, _expireSession);
  }

  Future<void> _saveLastActivity() async {
    final storage = ref.read(secureStorageProvider);
    await storage.saveLastActivity();
  }

  Future<void> _checkIfSessionExpired() async {
    final storage = ref.read(secureStorageProvider);
    final authState = ref.read(authStateProvider);

    // Solo verificar si hay sesión activa. Se usa valueOrNull porque
    // authStateProvider puede estar en estado de error (p.ej. un 401 al
    // refrescar el perfil) y AsyncValue.value relanza ese error en vez de
    // devolver null, lo que tumbaba este callback de ciclo de vida.
    if (authState.valueOrNull == null) return;

    final expired = await storage.isSessionExpired();
    if (expired && mounted) {
      _expireSession();
    } else {
      _resetTimer();
    }
  }

  void _expireSession() {
    _inactivityTimer?.cancel();
    final authState = ref.read(authStateProvider);

    // Solo actuar si hay sesión activa
    if (authState.valueOrNull == null) return;

    ref.read(authStateProvider.notifier).logout();

    if (mounted) {
      // Mostrar mensaje al usuario
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Tu sesión expiró por inactividad. Ingresa tu PIN nuevamente.'),
          duration: Duration(seconds: 4),
        ),
      );
      context.go('/login');
    }
  }

  @override
  Widget build(BuildContext context) {
    // Escuchar cualquier toque o movimiento del usuario en la pantalla
    return Listener(
      onPointerDown: (_) => _resetTimer(),
      onPointerMove: (_) => _resetTimer(),
      behavior: HitTestBehavior.translucent,
      child: widget.child,
    );
  }
}
