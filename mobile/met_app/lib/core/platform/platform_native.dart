import 'dart:io';

import 'package:crypto/crypto.dart';
import 'package:dio/dio.dart';
import 'package:dio/io.dart';
import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

/// Nombre de plataforma para el header `X-Platform`.
String currentPlatformName() {
  if (Platform.isAndroid) return 'android';
  if (Platform.isIOS) return 'ios';
  return 'unknown';
}

/// Los widget tests exportan `FLUTTER_TEST=true` en el entorno del proceso.
bool isRunningInFlutterTest() =>
    Platform.environment.containsKey('FLUTTER_TEST');

/// 🔒 SSL Pinning real en Dio (anti man-in-the-middle).
///
/// Con `SecurityContext(withTrustedRoots: false)` Dart desconfía de todos los
/// certificados (incluidos los del SO) y fuerza a que cada conexión pase por
/// `badCertificateCallback`, donde comparamos el SHA-256 del certificado
/// recibido contra la huella esperada ([AppConfig.sslFingerprint]).
void configureDioSecurity(Dio dio, String expectedFingerprint) {
  dio.httpClientAdapter = IOHttpClientAdapter(
    createHttpClient: () {
      final SecurityContext context = SecurityContext(withTrustedRoots: false);
      final client = HttpClient(context: context);

      client.badCertificateCallback =
          (X509Certificate cert, String host, int port) {
        // En debug aceptamos cualquier certificado (ngrok, túneles, etc.).
        if (kDebugMode) return true;

        // Excepción para desarrollo local.
        if (host.contains('localhost') || host.contains('10.0.2.2')) {
          return true;
        }

        final digest = sha256.convert(cert.der);
        final actualFingerprint = digest.bytes
            .map((b) => b.toRadixString(16).padLeft(2, '0').toUpperCase())
            .join(':');

        if (actualFingerprint == expectedFingerprint) {
          return true; // Es nuestro servidor legítimo.
        }

        debugPrint(
            '🚨 [CRITICAL] SSL Pinning falló. Posible MITM para host $host');
        return false;
      };
      return client;
    },
  );
}

/// Escribe los bytes a un archivo temporal y abre la hoja de compartir del SO.
Future<void> saveAndShareBytes(
  List<int> bytes,
  String fileName,
  String mimeType,
) async {
  final dir = await getTemporaryDirectory();
  final file = File('${dir.path}/$fileName');
  await file.writeAsBytes(bytes, flush: true);
  await Share.shareXFiles([XFile(file.path, mimeType: mimeType)]);
}
