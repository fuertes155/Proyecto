class SimulateLoanRequest {
  SimulateLoanRequest({
    required this.amount,
    required this.termMonths,
    this.annualInterestRate,
  });

  final double amount;
  final int termMonths;
  final double? annualInterestRate;

  Map<String, dynamic> toJson() => {
        'amount': amount,
        'termMonths': termMonths,
        if (annualInterestRate != null) 'annualInterestRate': annualInterestRate,
      };
}

class SubmitLoanApplicationRequest {
  SubmitLoanApplicationRequest({
    required this.amount,
    required this.termMonths,
    required this.purpose,
    this.annualInterestRate,
  });

  final double amount;
  final int termMonths;
  final String purpose;
  final double? annualInterestRate;

  Map<String, dynamic> toJson() => {
        'amount': amount,
        'termMonths': termMonths,
        'purpose': purpose,
        if (annualInterestRate != null) 'annualInterestRate': annualInterestRate,
      };
}

class AmortizationInstallment {
  AmortizationInstallment({
    required this.installmentNumber,
    required this.paymentAmount,
    required this.principalAmount,
    required this.interestAmount,
    required this.remainingBalance,
    required this.dueDate,
  });

  factory AmortizationInstallment.fromJson(Map<String, dynamic> json) {
    return AmortizationInstallment(
      installmentNumber: json['installmentNumber'] as int,
      paymentAmount: (json['paymentAmount'] as num).toDouble(),
      principalAmount: (json['principalAmount'] as num).toDouble(),
      interestAmount: (json['interestAmount'] as num).toDouble(),
      remainingBalance: (json['remainingBalance'] as num).toDouble(),
      dueDate: json['dueDate'] as String,
    );
  }

  final int installmentNumber;
  final double paymentAmount;
  final double principalAmount;
  final double interestAmount;
  final double remainingBalance;
  final String dueDate;
}

class LoanSimulationResult {
  LoanSimulationResult({
    required this.amount,
    required this.termMonths,
    required this.annualInterestRate,
    required this.monthlyPayment,
    required this.totalInterest,
    required this.totalPayment,
    required this.schedule,
  });

  factory LoanSimulationResult.fromJson(Map<String, dynamic> json) {
    return LoanSimulationResult(
      amount: (json['amount'] as num).toDouble(),
      termMonths: json['termMonths'] as int,
      annualInterestRate: (json['annualInterestRate'] as num).toDouble(),
      monthlyPayment: (json['monthlyPayment'] as num).toDouble(),
      totalInterest: (json['totalInterest'] as num).toDouble(),
      totalPayment: (json['totalPayment'] as num).toDouble(),
      schedule: (json['schedule'] as List)
          .map((e) => AmortizationInstallment.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  final double amount;
  final int termMonths;
  final double annualInterestRate;
  final double monthlyPayment;
  final double totalInterest;
  final double totalPayment;
  final List<AmortizationInstallment> schedule;
}

class LoanApplication {
  LoanApplication({
    required this.id,
    required this.amount,
    required this.termMonths,
    required this.monthlyPayment,
    required this.totalPayment,
    required this.purpose,
    required this.status,
    required this.submittedAt,
    this.schedule = const [],
  });

  factory LoanApplication.fromJson(Map<String, dynamic> json) {
    return LoanApplication(
      id: json['id'] as String,
      amount: (json['amount'] as num).toDouble(),
      termMonths: json['termMonths'] as int,
      monthlyPayment: (json['monthlyPayment'] as num).toDouble(),
      totalPayment: (json['totalPayment'] as num).toDouble(),
      purpose: json['purpose'] as String,
      status: json['status'] as String,
      submittedAt: json['submittedAt'] as String,
      schedule: json['schedule'] != null
          ? (json['schedule'] as List)
              .map((e) => AmortizationInstallment.fromJson(e as Map<String, dynamic>))
              .toList()
          : [],
    );
  }

  final String id;
  final double amount;
  final int termMonths;
  final double monthlyPayment;
  final double totalPayment;
  final String purpose;
  final String status;
  final String submittedAt;
  final List<AmortizationInstallment> schedule;

  String get statusLabel => switch (status) {
        'SUBMITTED' => 'Enviada',
        'IN_REVIEW' => 'En revisión',
        'APPROVED' => 'Aprobada',
        'REJECTED' => 'Rechazada',
        'DISBURSED' => 'Desembolsada',
        _ => status,
      };
}
