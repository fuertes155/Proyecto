import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:local_auth/local_auth.dart';

import '../providers/auth_provider.dart';

class SplashPage extends ConsumerStatefulWidget {
  const SplashPage({super.key});

  @override
  ConsumerState<SplashPage> createState() => _SplashPageState();
}

class _SplashPageState extends ConsumerState<SplashPage> {
  @override
  void initState() {
    super.initState();
    Future.microtask(_bootstrap);
  }

  Future<void> _bootstrap() async {
    await ref.read(authStateProvider.notifier).checkSession();
    if (!mounted) return;

    final authState = ref.read(authStateProvider);
    final hasSession = authState.hasValue && authState.value != null;
    if (!hasSession) {
      context.go('/login');
      return;
    }

    // Tener un token de sesión válido NO es suficiente para dar acceso: si
    // alguien más toma el teléfono desbloqueado, no debe poder abrir la app
    // directo a la billetera. Se exige biometría o PIN/patrón del dispositivo
    // (segundo factor local) antes de continuar a /home.
    final passedLocalAuth = await _verifyLocalFactor();
    if (!mounted) return;
    context.go(passedLocalAuth ? '/home' : '/login');
  }

  Future<bool> _verifyLocalFactor() async {
    if (kIsWeb) return true; // local_auth no aplica en la versión web
    try {
      final auth = LocalAuthentication();
      final supported = await auth.isDeviceSupported();
      if (!supported) return true; // sin lector biométrico ni bloqueo configurado: no se puede exigir
      return await auth.authenticate(
        localizedReason: 'Confirma tu identidad para entrar a Met',
        options: const AuthenticationOptions(stickyAuth: true),
      );
    } catch (_) {
      return false;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        width: double.infinity,
        // Mismo degradado que el login, para que la entrada a la app se sienta continua.
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Color(0xFF637C5A), Color(0xFF415739)],
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
          ),
        ),
        child: SafeArea(
          child: Column(
            children: [
              const Spacer(flex: 3),
              Container(
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(30),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withValues(alpha: 0.18),
                      blurRadius: 30,
                      offset: const Offset(0, 14),
                    ),
                  ],
                ),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(30),
                  child: Image.asset(
                    'assets/images/logo.png',
                    width: 116,
                    height: 116,
                    // El PNG original mide 6250 px: se decodifica al tamaño en que se muestra.
                    cacheWidth: 348,
                    semanticLabel: 'Logo de Met',
                    errorBuilder: (context, error, stackTrace) =>
                        const Icon(Icons.account_balance, size: 72, color: Colors.white),
                  ),
                ),
              ),
              const SizedBox(height: 24),
              const Text(
                'Met',
                style: TextStyle(color: Colors.white, fontSize: 34, fontWeight: FontWeight.w700, letterSpacing: 0.5),
              ),
              const SizedBox(height: 6),
              Text(
                'Ahorro, crédito e inversión',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.78), fontSize: 15),
              ),
              const Spacer(flex: 4),
              const SizedBox(
                width: 26,
                height: 26,
                child: CircularProgressIndicator(strokeWidth: 2.5, color: Colors.white),
              ),
              const SizedBox(height: 14),
              Text(
                'Preparando tu cuenta…',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.72), fontSize: 13),
              ),
              const SizedBox(height: 36),
            ],
          ),
        ),
      ),
    );
  }
}
