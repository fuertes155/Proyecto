class ExternalBankAccountModel {
  ExternalBankAccountModel({
    required this.id,
    required this.bankCode,
    required this.bankName,
    required this.accountType,
    required this.maskedAccountNumber,
    required this.verificationStatus,
    required this.verificationPending,
    required this.createdAt,
  });

  final String id;
  final String bankCode;
  final String bankName;
  final String accountType;
  final String maskedAccountNumber;
  final String verificationStatus;

  /// true si ya se envió un micro-depósito y está esperando que el usuario
  /// confirme el monto (ver ConfirmBankAccountVerificationUseCase en el backend).
  final bool verificationPending;
  final String createdAt;

  factory ExternalBankAccountModel.fromJson(Map<String, dynamic> json) {
    return ExternalBankAccountModel(
      id: json['id'] as String,
      bankCode: json['bankCode'] as String,
      bankName: json['bankName'] as String,
      accountType: json['accountType'] as String,
      maskedAccountNumber: json['maskedAccountNumber'] as String,
      verificationStatus: json['verificationStatus'] as String,
      verificationPending: json['verificationPending'] as bool? ?? false,
      createdAt: json['createdAt'] as String,
    );
  }

  String get accountTypeLabel => accountType == 'SAVINGS' ? 'Ahorros' : 'Corriente';

  String get statusLabel => switch (verificationStatus) {
        'VERIFIED' => 'Verificada',
        'PENDING' => 'En revisión',
        'REJECTED' => 'Rechazada',
        _ => verificationStatus,
      };

  bool get isUsable => verificationStatus != 'REJECTED';
}
