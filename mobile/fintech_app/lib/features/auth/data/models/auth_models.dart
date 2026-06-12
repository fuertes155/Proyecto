class RegisterRequest {
  RegisterRequest({
    required this.documentType,
    required this.documentNumber,
    required this.email,
    required this.phone,
    required this.firstName,
    required this.lastName,
    required this.pin,
  });

  final String documentType;
  final String documentNumber;
  final String email;
  final String phone;
  final String firstName;
  final String lastName;
  final String pin;

  Map<String, dynamic> toJson() => {
        'documentType': documentType,
        'documentNumber': documentNumber,
        'email': email,
        'phone': phone,
        'firstName': firstName,
        'lastName': lastName,
        'pin': pin,
      };
}

class LoginRequest {
  LoginRequest({
    required this.documentType,
    required this.documentNumber,
    this.pin,
    this.biometricPayload,
  });

  final String documentType;
  final String documentNumber;
  final String? pin;
  final String? biometricPayload;

  Map<String, dynamic> toJson() => {
        'documentType': documentType,
        'documentNumber': documentNumber,
        if (pin != null) 'pin': pin,
        if (biometricPayload != null) 'biometricPayload': biometricPayload,
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
    required this.status,
  });

  factory UserResponse.fromJson(Map<String, dynamic> json) {
    return UserResponse(
      id: json['id'] as String,
      firstName: json['firstName'] as String,
      lastName: json['lastName'] as String,
      email: json['email'] as String,
      status: json['status'] as String,
    );
  }

  final String id;
  final String firstName;
  final String lastName;
  final String email;
  final String status;

  String get fullName => '$firstName $lastName';
}
