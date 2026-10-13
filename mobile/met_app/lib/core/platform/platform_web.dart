import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:share_plus/share_plus.dart';

/// Nombre de plataforma para el header `X-Platform`.
String currentPlatformName() => 'web';

/// En web no hay `Platform.environment`; los widget tests corren sobre la VM.
bool isRunningInFlutterTest() => false;

/// En web Dio usa el adaptador del navegador (fetch), que no permite pinning de
/// certificados a nivel de aplicación. No-op.
void configureDioSecurity(Dio dio, String expectedFingerprint) {}

/// Comparte/descarga un archivo generado en memoria (no hay FS en el navegador).
Future<void> saveAndShareBytes(
  List<int> bytes,
  String fileName,
  String mimeType,
) async {
  await Share.shareXFiles([
    XFile.fromData(
      Uint8List.fromList(bytes),
      name: fileName,
      mimeType: mimeType,
    ),
  ]);
}
