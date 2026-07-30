class RegisterExternalBankAccountRequest {
  RegisterExternalBankAccountRequest({
    required this.bankCode,
    required this.accountType,
    required this.accountNumber,
  });

  final String bankCode;
  final String accountType;
  final String accountNumber;

  Map<String, dynamic> toJson() => {
        'bankCode': bankCode,
        'accountType': accountType,
        'accountNumber': accountNumber,
      };
}
