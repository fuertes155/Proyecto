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
    required this.hasAcceptedHabeasData,
    this.annualInterestRate,
  });

  final double amount;
  final int termMonths;
  final String purpose;
  final bool hasAcceptedHabeasData;
  final double? annualInterestRate;

  Map<String, dynamic> toJson() => {
        'amount': amount,
        'termMonths': termMonths,
        'purpose': purpose,
        'hasAcceptedHabeasData': hasAcceptedHabeasData,
        if (annualInterestRate != null) 'annualInterestRate': annualInterestRate,
      };
}

class LoanEligibilityRequest {
  LoanEligibilityRequest({required this.acceptedHabeasData});

  final bool acceptedHabeasData;

  Map<String, dynamic> toJson() => {'acceptedHabeasData': acceptedHabeasData};
}

/// Límites de crédito personalizados según el score real del usuario en
/// DataCrédito y su saldo de ahorro (ver `CreditScoringEngine` en backend).
class LoanEligibility {
  LoanEligibility({
    required this.approved,
    required this.tier,
    required this.score,
    required this.maxAmount,
    required this.maxTermMonths,
    required this.annualInterestRate,
    this.reason,
  });

  factory LoanEligibility.fromJson(Map<String, dynamic> json) {
    return LoanEligibility(
      approved: json['approved'] as bool,
      tier: json['tier'] as String,
      score: json['score'] as int,
      maxAmount: (json['maxAmount'] as num).toDouble(),
      maxTermMonths: json['maxTermMonths'] as int,
      annualInterestRate: (json['annualInterestRate'] as num).toDouble(),
      reason: json['reason'] as String?,
    );
  }

  final bool approved;
  final String tier;
  final int score;
  final double maxAmount;
  final int maxTermMonths;
  final double annualInterestRate;
  final String? reason;

  String get tierLabel => switch (tier) {
        'PRIME' => 'Prime',
        'RIESGO_BAJO' => 'Riesgo bajo',
        'RIESGO_MEDIO' => 'Riesgo medio',
        'RIESGO_ALTO' => 'Riesgo alto',
        'RECHAZADO' => 'No calificas',
        _ => tier,
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
    this.status = 'PENDING',
    this.penaltyInterestAmount = 0.0,
  });

  factory AmortizationInstallment.fromJson(Map<String, dynamic> json) {
    return AmortizationInstallment(
      installmentNumber: json['installmentNumber'] as int,
      paymentAmount: (json['paymentAmount'] as num).toDouble(),
      principalAmount: (json['principalAmount'] as num).toDouble(),
      interestAmount: (json['interestAmount'] as num).toDouble(),
      remainingBalance: (json['remainingBalance'] as num).toDouble(),
      dueDate: json['dueDate'] as String,
      status: json['status'] as String? ?? 'PENDING',
      penaltyInterestAmount: (json['penaltyInterestAmount'] as num?)?.toDouble() ?? 0.0,
    );
  }

  final int installmentNumber;
  final double paymentAmount;
  final double principalAmount;
  final double interestAmount;
  final double remainingBalance;
  final String dueDate;
  final String status;
  final double penaltyInterestAmount;
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
