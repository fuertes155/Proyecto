import 'package:flutter_test/flutter_test.dart';
import 'package:met/features/auth/data/models/auth_models.dart';

void main() {
  group('RegisterRequest', () {
    test('toJson envía solo documento, nombre completo, PIN y términos', () {
      final json = RegisterRequest(
        documentType: 'CC',
        documentNumber: '1086578123',
        fullName: 'Juan Carlos Pabón Gómez',
        pin: '1234',
        termsAccepted: true,
      ).toJson();

      expect(json, {
        'documentType': 'CC',
        'documentNumber': '1086578123',
        'fullName': 'Juan Carlos Pabón Gómez',
        'pin': '1234',
        'termsAccepted': true,
      });
    });
  });

  group('UserResponse', () {
    test('fromJson acepta una cuenta sin correo ni teléfono', () {
      // Así llega una cuenta recién creada: el correo se agrega después.
      final user = UserResponse.fromJson({
        'id': 'user-1',
        'firstName': 'Juan Carlos',
        'lastName': 'Pabón Gómez',
        'email': null,
        'phone': null,
        'status': 'ACTIVE',
        'kycStatus': 'PENDING',
      });

      expect(user.hasEmail, isFalse);
      expect(user.email, '');
      expect(user.phone, '');
      expect(user.fullName, 'Juan Carlos Pabón Gómez');
    });

    test('hasEmail es verdadero cuando el correo ya está registrado', () {
      final user = UserResponse.fromJson({
        'id': 'user-1',
        'firstName': 'Juan Carlos',
        'lastName': 'Pabón Gómez',
        'email': 'juan@example.com',
        'phone': '3001234567',
        'status': 'ACTIVE',
        'kycStatus': 'APPROVED',
      });

      expect(user.hasEmail, isTrue);
    });
  });
}
