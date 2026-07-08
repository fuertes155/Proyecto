import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/config/app_config.dart';
import 'core/router/app_router.dart';
import 'core/theme/app_theme.dart';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_jailbreak_detection/flutter_jailbreak_detection.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
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
      themeMode: ThemeMode.light,
      routerConfig: router,
    );
  }
}
