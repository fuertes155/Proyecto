import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:encrypt/encrypt.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pointycastle/asymmetric/api.dart';

import '../network/api_client_provider.dart';

final rsaEncryptionServiceProvider = Provider<RsaEncryptionService>((ref) {
  return RsaEncryptionService(ref.watch(apiClientProvider));
});

class RsaEncryptionService {
  RsaEncryptionService(this._dio);

  final Dio _dio;
  String? _publicKeyBase64;
  Encrypter? _encrypter;

  Future<void> _fetchPublicKeyIfNeeded() async {
    if (_encrypter != null) return;
    try {
      final response = await _dio.get('/v1/auth/public-key');
      if (response.statusCode != 200) {
        throw Exception(
            'Error HTTP ${response.statusCode} al obtener la llave pública.');
      }

      final responseData = response.data;
      String publicKeyBase64;

      if (responseData is String) {
        final trimmed = responseData.trim();
        if (trimmed.startsWith('<!DOCTYPE html>') ||
            trimmed.startsWith('<html')) {
          throw Exception(
              'La API devolvió HTML en lugar de la llave pública; revisa API_BASE_URL y que el backend esté funcionando correctamente.');
        }

        // Some servers may return JSON as plain text, or directly return the key string.
        try {
          final decoded = jsonDecode(responseData);
          if (decoded is Map && decoded['publicKey'] is String) {
            publicKeyBase64 = decoded['publicKey'] as String;
          } else {
            publicKeyBase64 = responseData;
          }
        } catch (_) {
          publicKeyBase64 = responseData;
        }
      } else if (responseData is Map) {
        final value = responseData['publicKey'];
        if (value is String) {
          publicKeyBase64 = value;
        } else {
          throw Exception(
              'La respuesta del servidor no incluye una llave pública válida.');
        }
      } else {
        throw Exception(
            'Respuesta inesperada al obtener la llave pública del servidor.');
      }

      _publicKeyBase64 = publicKeyBase64;
      final parser = RSAKeyParser();
      // Add standard PEM headers to the raw base64 key
      final pem =
          '-----BEGIN PUBLIC KEY-----\n$_publicKeyBase64\n-----END PUBLIC KEY-----';
      final publicKey = parser.parse(pem) as RSAPublicKey;

      _encrypter = Encrypter(RSA(publicKey: publicKey));
    } on DioException {
      // Fallo de red/servidor (sin conexión, timeout, IP equivocada, 5xx...).
      // Se re-lanza tal cual para que ErrorMapper lo traduzca a un mensaje
      // accionable ("Sin conexión al servidor...") en vez de un genérico.
      rethrow;
    } catch (e) {
      throw Exception('Error al obtener la llave pública del servidor: $e');
    }
  }

  Future<String> encryptPin(String pin) async {
    if (pin.isEmpty) return pin;
    await _fetchPublicKeyIfNeeded();
    final encrypted = _encrypter!.encrypt(pin);
    return encrypted.base64;
  }
}
