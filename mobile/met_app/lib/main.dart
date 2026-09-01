import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_jailbreak_detection/flutter_jailbreak_detection.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'core/platform/platform.dart';
import 'core/security/native_hardening.dart';
import 'core/config/app_config.dart';
import 'core/notifications/push_notification_service.dart';
import 'core/router/app_router.dart';
import 'core/session/inactivity_detector.dart';
import 'core/theme/app_theme.dart';
import 'core/widgets/app_error_widget.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Si el build de una pantalla lanza una excepción no capturada, mostrar una
  // tarjeta amable en vez del pantallazo rojo con el stacktrace.
  ErrorWidget.builder = (FlutterErrorDetails details) => AppErrorView(details: details);
  final priorOnError = FlutterError.onError;
  FlutterError.onError = (FlutterErrorDetails details) {
    // Se registra (consola / Crashlytics a futuro) pero no tumba la app.
    priorOnError?.call(details);
  };

  // DateFormat con locale explícito (p. ej. 'es_CO' en la pantalla de
  // movimientos) lanza LocaleDataException si esto no se llama antes:
  // los símbolos de mes/día en español no vienen cargados por defecto.
  await initializeDateFormatting('es_CO', null);

  // Carga el override de URL del servidor (pantalla de desarrollador del login)
  // ANTES de runApp, para que el cliente HTTP se construya con la URL correcta.
  await AppConfig.loadRuntimeOverrides();

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

  // Endurecimiento nativo (FLAG_SECURE + freeRASP/Talsec). No-op en web.
  await applyNativeHardening();

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
    if (!kIsWeb && !isRunningInFlutterTest()) {
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
