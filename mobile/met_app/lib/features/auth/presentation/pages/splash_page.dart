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
    return const Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.account_balance, size: 72, semanticLabel: 'Logo cooperativa'),
            SizedBox(height: 24),
            CircularProgressIndicator(),
            SizedBox(height: 16),
            Text('Cargando tu billetera cooperativa...', style: TextStyle(fontSize: 18)),
          ],
        ),
      ),
    );
  }
}
