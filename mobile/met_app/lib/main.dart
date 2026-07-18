import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:io';
import 'package:flutter_jailbreak_detection/flutter_jailbreak_detection.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_windowmanager/flutter_windowmanager.dart';
import 'package:freerasp/freerasp.dart';

import 'core/config/app_config.dart';
import 'core/router/app_router.dart';
import 'core/session/inactivity_detector.dart';
import 'core/theme/app_theme.dart';


Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  if (!kIsWeb && Platform.isAndroid) {
    try {
      await FlutterWindowManager.addFlags(FlutterWindowManager.FLAG_SECURE);
    } catch (e) {
      debugPrint("Error setting secure flag: $e");
    }
  }

  // Configuración de freeRASP (Detección de Emuladores, Debuggers y Hooks)
  if (!kIsWeb) {
    final config = TalsecConfig(
      androidConfig: AndroidConfig(
        packageName: 'com.cooperativa.met', // Reemplazar con real
        signingCertHashes: ['REPLACE_WITH_SHA256_HASH'],
      ),
      iosConfig: IOSConfig(
        bundleIds: ['com.cooperativa.met'],
        teamId: 'REPLACE_TEAM_ID',
      ),
      watcherMail: 'security@cooperativa.met.com',
      isProd: true,
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
                  style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.white),
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

class MetApp extends ConsumerWidget {
  const MetApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
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
