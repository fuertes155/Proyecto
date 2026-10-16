import 'dart:convert';
import 'dart:math';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:local_auth/local_auth.dart';

import '../../../../core/storage/secure_storage_service.dart';
import '../../domain/repositories/auth_repository.dart';
import '../repositories/auth_repository_impl.dart';

final biometricLoginServiceProvider = Provider<BiometricLoginService>((ref) {
  return BiometricLoginService(
    storage: ref.watch(secureStorageProvider),
    repository: ref.watch(authRepositoryProvider),
  );
});

/// Llave de huella guardada en este celular y el documento del socio al que pertenece.
typedef BiometricCredential = ({String token, String documentType, String documentNumber});

/// Ingreso con huella (o rostro).
///
/// Al activarlo, el celular genera una llave aleatoria, el servidor guarda solo su hash (previa
/// confirmación del PIN) y la llave queda en el almacenamiento seguro. Al entrar, el sistema pide
/// la huella y, si es correcta, la app envía la llave en lugar del PIN.
class BiometricLoginService {
  BiometricLoginService({
    required SecureStorageService storage,
    required AuthRepository repository,
    LocalAuthentication? localAuth,
  })  : _storage = storage,
        _repository = repository,
        _localAuth = localAuth ?? LocalAuthentication();

  final SecureStorageService _storage;
  final AuthRepository _repository;
  final LocalAuthentication _localAuth;

  /// El celular tiene al menos una huella o un rostro registrado. En la versión web no aplica.
  Future<bool> isAvailable() async {
    if (kIsWeb) return false;
    try {
      if (!await _localAuth.isDeviceSupported()) return false;
      final enrolled = await _localAuth.getAvailableBiometrics();
      return enrolled.isNotEmpty;
    } on PlatformException {
      return false;
    }
  }

  /// Muestra el diálogo del sistema. `false` si el socio lo cancela o la huella no coincide.
  Future<bool> authenticate(String reason) async {
    try {
      return await _localAuth.authenticate(
        localizedReason: reason,
        options: const AuthenticationOptions(biometricOnly: true, stickyAuth: true),
      );
    } on PlatformException {
      return false;
    }
  }

  Future<BiometricCredential?> readCredential() => _storage.readBiometricCredential();

  /// Activa la huella en este celular. Devuelve `false` si el socio cancela el diálogo de la huella;
  /// si el servidor rechaza el PIN, propaga el error.
  Future<bool> enable({
    required String pin,
    required String documentType,
    required String documentNumber,
  }) async {
    final confirmed = await authenticate('Pon tu huella para activar el ingreso con huella');
    if (!confirmed) return false;

    final token = _newToken();
    await _repository.enableBiometricLogin(pin: pin, biometricToken: token);
    await _storage.saveBiometricCredential(
      token: token,
      documentType: documentType,
      documentNumber: documentNumber,
    );
    return true;
  }

  Future<void> disable() async {
    await _repository.disableBiometricLogin();
    await forget();
  }

  /// Borra la llave solo de este celular (p. ej. si el servidor ya no la reconoce).
  Future<void> forget() => _storage.deleteBiometricCredential();

  /// Si otra persona entra con su PIN en este celular, la huella guardada de la anterior deja de
  /// ofrecerse: el botón de huella no debe abrir una cuenta distinta a la de quien usa el celular.
  Future<void> forgetIfBelongsToAnother({required String documentType, required String documentNumber}) async {
    final credential = await readCredential();
    if (credential == null) return;
    if (credential.documentType != documentType || credential.documentNumber != documentNumber) {
      await forget();
    }
  }

  static String _newToken() {
    final random = Random.secure();
    return base64UrlEncode(List<int>.generate(32, (_) => random.nextInt(256)));
  }
}
