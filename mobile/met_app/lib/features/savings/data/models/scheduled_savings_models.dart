class CreateScheduledSavingsRequest {
  CreateScheduledSavingsRequest({
    required this.name,
    required this.contributionAmount,
    required this.frequency,
    this.targetAmount,
    this.debitDayOfWeek,
    this.debitDayOfMonth,
  });

  final String name;
  final double contributionAmount;
  final double? targetAmount;
  final String frequency;
  final int? debitDayOfWeek;
  final int? debitDayOfMonth;

  Map<String, dynamic> toJson() => {
        'name': name,
        'contributionAmount': contributionAmount,
        if (targetAmount != null) 'targetAmount': targetAmount,
        'frequency': frequency,
        if (debitDayOfWeek != null) 'debitDayOfWeek': debitDayOfWeek,
        if (debitDayOfMonth != null) 'debitDayOfMonth': debitDayOfMonth,
      };
}

class UpdateScheduledSavingsRequest {
  UpdateScheduledSavingsRequest({this.contributionAmount, this.status});

  final double? contributionAmount;
  final String? status;

  Map<String, dynamic> toJson() => {
        if (contributionAmount != null) 'contributionAmount': contributionAmount,
        if (status != null) 'status': status,
      };
}

class ScheduledSavingsAccount {
  ScheduledSavingsAccount({
    required this.id,
    required this.name,
    required this.contributionAmount,
    required this.frequency,
    required this.currentBalance,
    required this.progressPercentage,
    required this.status,
    required this.nextContributionDate,
    this.targetAmount,
    this.debitDayOfWeek,
    this.debitDayOfMonth,
  });

  factory ScheduledSavingsAccount.fromJson(Map<String, dynamic> json) {
    return ScheduledSavingsAccount(
      id: json['id'] as String,
      name: json['name'] as String,
      targetAmount: (json['targetAmount'] as num?)?.toDouble(),
      contributionAmount: (json['contributionAmount'] as num).toDouble(),
      frequency: json['frequency'] as String,
      debitDayOfWeek: json['debitDayOfWeek'] as int?,
      debitDayOfMonth: json['debitDayOfMonth'] as int?,
      currentBalance: (json['currentBalance'] as num).toDouble(),
      progressPercentage: (json['progressPercentage'] as num).toDouble(),
      status: json['status'] as String,
      nextContributionDate: json['nextContributionDate'] as String,
    );
  }

  final String id;
  final String name;
  final double? targetAmount;
  final double contributionAmount;
  final String frequency;
  final int? debitDayOfWeek;
  final int? debitDayOfMonth;
  final double currentBalance;
  final double progressPercentage;
  final String status;
  final String nextContributionDate;

  String get frequencyLabel => switch (frequency) {
        'WEEKLY' => 'Semanal',
        'BIWEEKLY' => 'Quincenal',
        'MONTHLY' => 'Mensual',
        _ => frequency,
      };

  String get statusLabel => switch (status) {
        'ACTIVE' => 'Activa',
        'PAUSED' => 'Pausada',
        'COMPLETED' => 'Completada',
        'CANCELLED' => 'Cancelada',
        _ => status,
      };
}

class ContributionItem {
  ContributionItem({
    required this.id,
    required this.amount,
    required this.scheduledDate,
    required this.status,
    this.executedAt,
    this.failureReason,
  });

  factory ContributionItem.fromJson(Map<String, dynamic> json) {
    return ContributionItem(
      id: json['id'] as String,
      amount: (json['amount'] as num).toDouble(),
      scheduledDate: json['scheduledDate'] as String,
      executedAt: json['executedAt'] as String?,
      status: json['status'] as String,
      failureReason: json['failureReason'] as String?,
    );
  }

  final String id;
  final double amount;
  final String scheduledDate;
  final String? executedAt;
  final String status;
  final String? failureReason;

  String get statusLabel => switch (status) {
        'COMPLETED' => 'Completado',
        'PENDING' => 'Pendiente',
        'FAILED' => 'Fallido',
        _ => status,
      };
}
