class CreateSolidarityGroupRequest {
  CreateSolidarityGroupRequest({
    required this.name,
    this.description,
    this.minContribution,
    this.maxMembers,
  });

  final String name;
  final String? description;
  final double? minContribution;
  final int? maxMembers;

  Map<String, dynamic> toJson() => {
        'name': name,
        if (description != null) 'description': description,
        if (minContribution != null) 'minContribution': minContribution,
        if (maxMembers != null) 'maxMembers': maxMembers,
      };
}

class JoinSolidarityGroupRequest {
  JoinSolidarityGroupRequest({required this.inviteCode});
  final String inviteCode;
  Map<String, dynamic> toJson() => {'inviteCode': inviteCode};
}

class ContributeToPoolRequest {
  ContributeToPoolRequest({required this.amount});
  final double amount;
  Map<String, dynamic> toJson() => {'amount': amount};
}

class RequestMicroLoanRequest {
  RequestMicroLoanRequest({
    required this.amount,
    required this.purpose,
    required this.termMonths,
  });

  final double amount;
  final String purpose;
  final int termMonths;

  Map<String, dynamic> toJson() => {
        'amount': amount,
        'purpose': purpose,
        'termMonths': termMonths,
      };
}

class ReviewMicroLoanRequest {
  ReviewMicroLoanRequest({required this.approved, this.rejectionReason});
  final bool approved;
  final String? rejectionReason;

  Map<String, dynamic> toJson() => {
        'approved': approved,
        if (rejectionReason != null) 'rejectionReason': rejectionReason,
      };
}

class SolidarityGroup {
  SolidarityGroup({
    required this.id,
    required this.name,
    required this.inviteCode,
    required this.minContribution,
    required this.poolBalance,
    required this.maxLoanAmount,
    required this.memberCount,
    required this.maxMembers,
    required this.status,
    required this.myRole,
    this.description,
    this.interestRate,
  });

  factory SolidarityGroup.fromJson(Map<String, dynamic> json) {
    return SolidarityGroup(
      id: json['id'] as String,
      name: json['name'] as String,
      description: json['description'] as String?,
      inviteCode: json['inviteCode'] as String,
      minContribution: (json['minContribution'] as num).toDouble(),
      poolBalance: (json['poolBalance'] as num).toDouble(),
      maxLoanAmount: (json['maxLoanAmount'] as num).toDouble(),
      memberCount: json['memberCount'] as int,
      maxMembers: json['maxMembers'] as int,
      status: json['status'] as String,
      myRole: json['myRole'] as String,
      interestRate: (json['interestRate'] as num?)?.toDouble(),
    );
  }

  final String id;
  final String name;
  final String? description;
  final String inviteCode;
  final double minContribution;
  final double poolBalance;
  final double maxLoanAmount;
  final int memberCount;
  final int maxMembers;
  final String status;
  final String myRole;
  final double? interestRate;

  bool get isAdmin => myRole == 'ADMIN';
}

class MicroLoan {
  MicroLoan({
    required this.id,
    required this.borrowerId,
    required this.amount,
    required this.purpose,
    required this.termMonths,
    required this.status,
    required this.requestedAt,
  });

  factory MicroLoan.fromJson(Map<String, dynamic> json) {
    return MicroLoan(
      id: json['id'] as String,
      borrowerId: json['borrowerId'] as String,
      amount: (json['amount'] as num).toDouble(),
      purpose: json['purpose'] as String,
      termMonths: json['termMonths'] as int,
      status: json['status'] as String,
      requestedAt: json['requestedAt'] as String,
    );
  }

  final String id;
  final String borrowerId;
  final double amount;
  final String purpose;
  final int termMonths;
  final String status;
  final String requestedAt;

  String get statusLabel => switch (status) {
        'PENDING' => 'Pendiente',
        'APPROVED' => 'Aprobado',
        'DISBURSED' => 'Desembolsado',
        'REPAID' => 'Pagado',
        'REJECTED' => 'Rechazado',
        _ => status,
      };
}

class LoanInstallment {
  LoanInstallment({
    required this.id,
    required this.installmentNumber,
    required this.totalAmount,
    required this.dueDate,
    required this.status,
  });

  factory LoanInstallment.fromJson(Map<String, dynamic> json) {
    return LoanInstallment(
      id: json['id'] as String,
      installmentNumber: json['installmentNumber'] as int,
      totalAmount: (json['totalAmount'] as num).toDouble(),
      dueDate: json['dueDate'] as String,
      status: json['status'] as String,
    );
  }

  final String id;
  final int installmentNumber;
  final double totalAmount;
  final String dueDate;
  final String status;
}

class PoolTransaction {
  PoolTransaction({
    required this.type,
    required this.amount,
    required this.description,
    required this.createdAt,
  });

  factory PoolTransaction.fromJson(Map<String, dynamic> json) {
    return PoolTransaction(
      type: json['type'] as String,
      amount: (json['amount'] as num).toDouble(),
      description: json['description'] as String? ?? '',
      createdAt: json['createdAt'] as String,
    );
  }

  final String type;
  final double amount;
  final String description;
  final String createdAt;

  String get typeLabel => switch (type) {
        'CONTRIBUTION' => 'Aporte',
        'LOAN_DISBURSEMENT' => 'Desembolso',
        'LOAN_REPAYMENT' => 'Pago cuota',
        _ => type,
      };
}
