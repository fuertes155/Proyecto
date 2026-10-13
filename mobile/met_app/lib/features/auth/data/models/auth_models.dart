/// El registro solo pide cédula, nombre completo y PIN. El backend parte el
/// nombre en nombres/apellidos; el correo y el celular se agregan después
/// desde "Mis Datos Personales".
class RegisterRequest {
  RegisterRequest({
    required this.documentType,
    required this.documentNumber,
    required this.fullName,
    required this.pin,
    required this.termsAccepted,
  });

  final String documentType;
  final String documentNumber;
  final String fullName;
  final String pin;
  final bool termsAccepted;

  Map<String, dynamic> toJson() => {
        'documentType': documentType,
        'documentNumber': documentNumber,
        'fullName': fullName,
        'pin': pin,
        'termsAccepted': termsAccepted,
      };
}

class LoginRequest {
  LoginRequest({
    required this.documentType,
    required this.documentNumber,
    this.deviceId,
    this.pin,
    this.biometricPayload,
    this.otpCode,
  });

  final String documentType;
  final String documentNumber;
  final String? deviceId;
  final String? pin;
  final String? biometricPayload;
  final String? otpCode;

  Map<String, dynamic> toJson() => {
        'documentType': documentType,
        'documentNumber': documentNumber,
        if (deviceId != null) 'deviceId': deviceId,
        if (pin != null) 'pin': pin,
        if (biometricPayload != null) 'biometricPayload': biometricPayload,
        if (otpCode != null) 'otpCode': otpCode,
      };
}

class AuthResponse {
  AuthResponse({
    required this.userId,
    required this.accessToken,
    required this.refreshToken,
    required this.expiresInMs,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    return AuthResponse(
      userId: json['userId'] as String,
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
      expiresInMs: json['expiresInMs'] as int,
    );
  }

  final String userId;
  final String accessToken;
  final String refreshToken;
  final int expiresInMs;
}

class UserResponse {
  UserResponse({
    required this.id,
    required this.firstName,
    required this.lastName,
    required this.email,
    required this.phone,
    required this.status,
    required this.kycStatus,
    required this.emailNotificationsEnabled,
    required this.pushNotificationsEnabled,
  });

  factory UserResponse.fromJson(Map<String, dynamic> json) {
    return UserResponse(
      id: json['id'] as String,
      firstName: json['firstName'] as String,
      lastName: json['lastName'] as String,
      // Una cuenta recién creada aún no tiene correo (ver RegisterRequest).
      email: json['email'] as String? ?? '',
      phone: json['phone'] as String? ?? '',
      status: json['status'] as String,
      kycStatus: json['kycStatus'] as String? ?? 'PENDING',
      emailNotificationsEnabled: json['emailNotificationsEnabled'] as bool? ?? true,
      pushNotificationsEnabled: json['pushNotificationsEnabled'] as bool? ?? true,
    );
  }

  final String id;
  final String firstName;
  final String lastName;
  final String email;
  final String phone;
  final String status;
  final String kycStatus;
  final bool emailNotificationsEnabled;
  final bool pushNotificationsEnabled;

  String get fullName => '$firstName $lastName';

  bool get hasEmail => email.isNotEmpty;
}

class PinRecoveryRequest {
  PinRecoveryRequest({
    required this.documentType,
    required this.documentNumber,
  });

  final String documentType;
  final String documentNumber;

  Map<String, dynamic> toJson() => {
        'documentType': documentType,
        'documentNumber': documentNumber,
      };
}

class PinRecoveryResetRequest {
  PinRecoveryResetRequest({
    required this.documentType,
    required this.documentNumber,
    required this.otpCode,
    required this.newPin,
  });

  final String documentType;
  final String documentNumber;
  final String otpCode;
  final String newPin;

  Map<String, dynamic> toJson() => {
        'documentType': documentType,
        'documentNumber': documentNumber,
        'otpCode': otpCode,
        'newPin': newPin,
      };
}
