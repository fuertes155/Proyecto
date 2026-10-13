import 'package:flutter_test/flutter_test.dart';
import 'package:met/features/auth/data/models/auth_models.dart';

void main() {
  group('RegisterRequest', () {
    test('toJson sends only document, full name, pin and terms', () {
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
    test('fromJson accepts an account without email or phone', () {
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

    test('hasEmail is true once the email is registered', () {
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
