import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:io';
import 'package:flutter_jailbreak_detection/flutter_jailbreak_detection.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_windowmanager_plus/flutter_windowmanager_plus.dart';
import 'package:freerasp/freerasp.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'core/config/app_config.dart';
import 'core/notifications/push_notification_service.dart';
import 'core/router/app_router.dart';
import 'core/session/inactivity_detector.dart';
import 'core/theme/app_theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // DateFormat con locale explícito (p. ej. 'es_CO' en la pantalla de
  // movimientos) lanza LocaleDataException si esto no se llama antes:
  // los símbolos de mes/día en español no vienen cargados por defecto.
  await initializeDateFormatting('es_CO', null);

  // Inicialización de Firebase
  if (!kIsWeb) {
    try {
      await Firebase.initializeApp();
    } catch (e) {
      debugPrint("Firebase init error: $e");
    }
  } else {
    debugPrint(
        "Skipping Firebase.initializeApp() on web. Configure Firebase web options before enabling Firebase on web.");
  }

  if (!kIsWeb && Platform.isAndroid) {
    try {
      await FlutterWindowManagerPlus.addFlags(FlutterWindowManagerPlus.FLAG_SECURE);
    } catch (e) {
      debugPrint("Error setting secure flag: $e");
    }
  }

  // Configuración de freeRASP (Detección de Emuladores, Debuggers y Hooks)
  if (!kIsWeb) {
    // ─────────────────────────────────────────────────────────────────────────
    // CONFIGURACIÓN freeRASP — VALORES REQUERIDOS ANTES DE PUBLICAR
    //
    // 1. ANDROID — signingCertHashes:
    //    Obtener con: keytool -printcert -jarfile app-release.apk | grep SHA256
    //    Formato: "AA:BB:CC:..." en minúsculas sin los dos puntos → "aabbcc..."
    //
    // 2. iOS — teamId:
    //    Obtener en: https://developer.apple.com/account → Membership → Team ID
    //
    // Los valores deben guardarse en variables de entorno del CI/CD
    // o en un archivo de configuración NO versionado (e.g., .env.local)
    // ─────────────────────────────────────────────────────────────────────────
    const androidSigningHash = String.fromEnvironment(
      'ANDROID_SIGNING_HASH',
      defaultValue: '', // Vacío en debug; debe pasarse en release build
    );
    const iosTeamId = String.fromEnvironment(
      'IOS_TEAM_ID',
      defaultValue: '', // Vacío en debug; debe pasarse en release build
    );

    // Guard: falla explícitamente en debug si los valores no están configurados
    assert(
      kDebugMode || androidSigningHash.isNotEmpty,
      '🔴 ANDROID_SIGNING_HASH no está configurado. '
      'Pasa --dart-define=ANDROID_SIGNING_HASH=<sha256> en el build de release.',
    );
    assert(
      kDebugMode || iosTeamId.isNotEmpty,
      '🔴 IOS_TEAM_ID no está configurado. '
      'Pasa --dart-define=IOS_TEAM_ID=<teamId> en el build de release.',
    );

    final config = TalsecConfig(
      androidConfig: AndroidConfig(
        packageName: 'com.cooperativa.met',
        signingCertHashes: androidSigningHash.isNotEmpty
            ? [androidSigningHash]
            // freeRASP valida en runtime que cada hash sea un SHA-256 en Base64 (32 bytes,
            // 44 chars) — un placeholder de texto libre como "__DEBUG_PLACEHOLDER__" lanza
            // "configuration-exception" SIN capturar durante la construcción de AndroidConfig
            // (antes del try/catch de Talsec.instance.start), lo que aborta main() en seco
            // antes de runApp() y deja la app congelada en el splash nativo para siempre.
            // Este es un Base64 de 32 ceros — pasa el formato pero freeRASP igual reportará
            // integridad inválida en dev (esperado; solo importa en un build de release real).
            : ['AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA='], // Solo válido en dev local
      ),
      iosConfig: IOSConfig(
        bundleIds: ['com.cooperativa.met'],
        teamId: iosTeamId.isNotEmpty ? iosTeamId : '__DEBUG_PLACEHOLDER__',
      ),
      watcherMail: 'security@cooperativa.met.com',
      isProd: !kDebugMode,
    );

    final callback = ThreatCallback(
      onAppIntegrity: () => debugPrint("THREAT: App Integrity"),
      onObfuscationIssues: () => debugPrint("THREAT: Obfuscation"),
      onDebug: () => debugPrint("THREAT: Debugging"),
      onHooks: () => debugPrint("THREAT: Hooks (Frida/Xposed)"),
      onPrivilegedAccess: () => debugPrint("THREAT: Root/Jailbreak"),
      onSimulator: () => debugPrint("THREAT: Simulator"),
    );

    Talsec.instance.attachListener(callback);
    try {
      await Talsec.instance.start(config);
    } catch (e) {
      debugPrint("freeRASP start error: $e");
    }
  }

  bool jailbroken = false;
  if (!kIsWeb) {
    try {
      jailbroken = await FlutterJailbreakDetection.jailbroken;
    } on PlatformException {
      jailbroken = true;
    }
  }

  if (jailbroken) {
    runApp(const SecurityAlertApp());
    return;
  }

  runApp(const ProviderScope(child: MetApp()));
}

class SecurityAlertApp extends StatelessWidget {
  const SecurityAlertApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        backgroundColor: Colors.red.shade900,
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.security, size: 80, color: Colors.white),
                const SizedBox(height: 24),
                const Text(
                  'Alerta de Seguridad',
                  style: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: Colors.white),
                ),
                const SizedBox(height: 16),
                const Text(
                  'Hemos detectado que este dispositivo está modificado (Root/Jailbreak) o presenta riesgos de seguridad graves.\n\nPor la protección de su dinero, la aplicación financiera no puede ejecutarse en este dispositivo.',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 16, color: Colors.white),
                ),
                const SizedBox(height: 32),
                ElevatedButton(
                  onPressed: () => SystemNavigator.pop(),
                  child: const Text('Cerrar Aplicación'),
                )
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class MetApp extends ConsumerStatefulWidget {
  const MetApp({super.key});

  @override
  ConsumerState<MetApp> createState() => _MetAppState();
}

class _MetAppState extends ConsumerState<MetApp> {
  @override
  void initState() {
    super.initState();
    // Inicializar notificaciones después del frame (cuando Riverpod está listo)
    // Se omite en modo test y en web para evitar llamadas a Platform channels no soportadas.
    if (!kIsWeb && !Platform.environment.containsKey('FLUTTER_TEST')) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        try {
          ref.read(pushNotificationServiceProvider).initialize();
        } catch (e) {
          debugPrint("Push notification init error (likely test env): $e");
        }
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final router = ref.watch(appRouterProvider);

    return MaterialApp.router(
      title: AppConfig.appName,
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: ref.watch(themeModeProvider),
      routerConfig: router,
      builder: (context, child) => InactivityDetector(
        child: child ?? const SizedBox.shrink(),
      ),
    );
  }
}
