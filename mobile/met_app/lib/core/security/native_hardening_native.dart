import 'package:flutter/foundation.dart';
import 'package:flutter_windowmanager_plus/flutter_windowmanager_plus.dart';
import 'package:freerasp/freerasp.dart';

/// Aplica el endurecimiento de seguridad nativo:
///  - `FLAG_SECURE` en Android release (bloquea capturas / grabación de pantalla
///     con datos financieros).
///  - freeRASP / Talsec: detección de emuladores, debuggers, hooks (Frida/Xposed),
///     root/jailbreak e integridad de la app.
Future<void> applyNativeHardening() async {
  if (defaultTargetPlatform == TargetPlatform.android && !kDebugMode) {
    try {
      await FlutterWindowManagerPlus.addFlags(
          FlutterWindowManagerPlus.FLAG_SECURE);
    } catch (e) {
      debugPrint('Error setting secure flag: $e');
    }
  }

  // ───────────────────────────────────────────────────────────────────────────
  // CONFIGURACIÓN freeRASP — VALORES REQUERIDOS ANTES DE PUBLICAR
  //
  // 1. ANDROID — signingCertHashes:
  //    Obtener con: keytool -printcert -jarfile app-release.apk | grep SHA256
  //    Formato: "AA:BB:CC:..." en minúsculas sin los dos puntos → "aabbcc..."
  //
  // 2. iOS — teamId:
  //    Obtener en: https://developer.apple.com/account → Membership → Team ID
  //
  // Los valores deben guardarse en variables de entorno del CI/CD o en un
  // archivo de configuración NO versionado (e.g., .env.local).
  // ───────────────────────────────────────────────────────────────────────────
  const androidSigningHash = String.fromEnvironment(
    'ANDROID_SIGNING_HASH',
    defaultValue: '', // Vacío en debug; debe pasarse en release build
  );
  const iosTeamId = String.fromEnvironment(
    'IOS_TEAM_ID',
    defaultValue: '', // Vacío en debug; debe pasarse en release build
  );

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
          // freeRASP valida en runtime que cada hash sea un SHA-256 en Base64
          // (32 bytes, 44 chars) — un placeholder de texto libre lanza
          // "configuration-exception" SIN capturar durante la construcción de
          // AndroidConfig (antes del try/catch de Talsec.instance.start), lo que
          // aborta main() en seco. Este es un Base64 de 32 ceros: pasa el
          // formato pero freeRASP reportará integridad inválida en dev
          // (esperado; solo importa en un build de release real).
          : ['AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA='],
    ),
    iosConfig: IOSConfig(
      bundleIds: ['com.cooperativa.met'],
      teamId: iosTeamId.isNotEmpty ? iosTeamId : '__DEBUG_PLACEHOLDER__',
    ),
    watcherMail: 'security@cooperativa.met.com',
    isProd: !kDebugMode,
  );

  final callback = ThreatCallback(
    onAppIntegrity: () => debugPrint('THREAT: App Integrity'),
    onObfuscationIssues: () => debugPrint('THREAT: Obfuscation'),
    onDebug: () => debugPrint('THREAT: Debugging'),
    onHooks: () => debugPrint('THREAT: Hooks (Frida/Xposed)'),
    onPrivilegedAccess: () => debugPrint('THREAT: Root/Jailbreak'),
    onSimulator: () => debugPrint('THREAT: Simulator'),
  );

  Talsec.instance.attachListener(callback);
  try {
    await Talsec.instance.start(config);
  } catch (e) {
    debugPrint('freeRASP start error: $e');
  }
}
