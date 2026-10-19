import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../../core/config/app_config.dart';

// Definición básica de los modelos para el Panel Administrativo
class UserModel {
  final String id;
  final String firstName;
  final String lastName;
  final String email;
  final String kycStatus;
  final String documentNumber;
  final String documentType;
  final String? phone;
  final String? status;
  final String? createdAt;

  UserModel({
    required this.id,
    required this.firstName,
    required this.lastName,
    required this.email,
    required this.kycStatus,
    required this.documentNumber,
    this.documentType = 'CC',
    this.phone,
    this.status,
    this.createdAt,
  });

  String get fullName => '$firstName $lastName'.trim();

  /// "CC 1.234.567" — tipo + número, como se ve en un documento.
  String get documentLabel => '$documentType $documentNumber';

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] ?? '',
      firstName: json['firstName'] ?? 'Sin nombre',
      lastName: json['lastName'] ?? '',
      email: json['email'] ?? '',
      kycStatus: json['kycStatus'] ?? 'PENDING',
      documentNumber: json['documentNumber'] ?? 'Sin documento',
      documentType: json['documentType']?.toString() ?? 'CC',
      phone: json['phone']?.toString(),
      status: json['status']?.toString(),
      createdAt: json['createdAt']?.toString(),
    );
  }
}

class AccountModel {
  final String id;
  final String accountNumber;
  final String userId;
  final String status;
  final double balance;
  
  AccountModel({required this.id, required this.accountNumber, required this.userId, required this.status, required this.balance});
  
  factory AccountModel.fromJson(Map<String, dynamic> json) {
    return AccountModel(
      id: json['id'] ?? '',
      accountNumber: json['accountNumber'] ?? '',
      userId: json['userId'] ?? '',
      status: json['status'] ?? 'ACTIVE',
      balance: double.tryParse(json['principalBalance']?.toString() ?? '0') ?? 0.0,
    );
  }
}

class LoanModel {
  final String id;
  final String userId;
  final String status;
  final double? amount;
  final int? termMonths;
  final double? monthlyPayment;
  final String? purpose;
  final String? riskTier;
  final int? creditScore;
  final Object? submittedAt;

  LoanModel({
    required this.id,
    required this.userId,
    required this.status,
    this.amount,
    this.termMonths,
    this.monthlyPayment,
    this.purpose,
    this.riskTier,
    this.creditScore,
    this.submittedAt,
  });

  factory LoanModel.fromJson(Map<String, dynamic> json) {
    double? toDouble(Object? v) => v == null ? null : double.tryParse(v.toString());
    int? toInt(Object? v) => v == null ? null : int.tryParse(v.toString());
    return LoanModel(
      id: json['id'] ?? '',
      userId: json['userId'] ?? '',
      status: json['status'] ?? 'PENDING',
      amount: toDouble(json['amount']),
      termMonths: toInt(json['termMonths']),
      monthlyPayment: toDouble(json['monthlyPayment']),
      purpose: json['purpose']?.toString(),
      riskTier: json['riskTier']?.toString(),
      creditScore: toInt(json['creditScore']),
      submittedAt: json['submittedAt'] ?? json['createdAt'],
    );
  }
}

final adminApiClientProvider = Provider<Dio>((ref) {
  // Misma URL base que el resto del panel (AppConfig). API_URL se sigue aceptando porque
  // publicar-apks.ps1 la pasa al compilar la APK de administrador.
  const fromEnv = String.fromEnvironment('API_URL');
  final baseUrl = fromEnv.isNotEmpty ? fromEnv : '${AppConfig.apiBaseUrl}/v1/admin';

  final dio = Dio(BaseOptions(
    baseUrl: baseUrl,
    connectTimeout: const Duration(seconds: 5),
  ));
  
  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) async {
      const storage = FlutterSecureStorage();
      final token = await storage.read(key: 'admin_access_token');
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      }
      return handler.next(options);
    }
  ));
  
  return dio;
});

// Providers de datos
final usersProvider = FutureProvider<List<UserModel>>((ref) async {
  try {
    final dio = ref.read(adminApiClientProvider);
    final response = await dio.get('/users');
    final List data = response.data;
    return data.map((json) => UserModel.fromJson(json)).toList();
  } catch (e) {
    debugPrint('Error fetching users: $e');
    rethrow;
  }
});

final accountsProvider = FutureProvider<List<AccountModel>>((ref) async {
  try {
    final dio = ref.read(adminApiClientProvider);
    final response = await dio.get('/accounts');
    final List data = response.data;
    return data.map((json) => AccountModel.fromJson(json)).toList();
  } catch (e) {
    debugPrint('Error fetching accounts: $e');
    rethrow;
  }
});

final accountsWithUsersProvider = FutureProvider<List<Map<String, dynamic>>>((ref) async {
  final accounts = await ref.watch(accountsProvider.future);
  final users = await ref.watch(usersProvider.future);
  
  return accounts.map((account) {
    final user = users.firstWhere(
      (u) => u.id == account.userId,
      orElse: () => UserModel(
        id: account.userId,
        firstName: 'Desconocido',
        lastName: '',
        email: '',
        kycStatus: '',
        documentNumber: 'N/A'
      ),
    );
    return {
      'account': account,
      'user': user,
    };
  }).toList();
});

final loansProvider = FutureProvider<List<LoanModel>>((ref) async {
  try {
    final dio = ref.read(adminApiClientProvider);
    final response = await dio.get('/loans');
    final List data = response.data;
    return data.map((json) => LoanModel.fromJson(json)).toList();
  } catch (e) {
    debugPrint('Error fetching loans: $e');
    rethrow;
  }
});

final loansWithUsersProvider = FutureProvider<List<Map<String, dynamic>>>((ref) async {
  final loans = await ref.watch(loansProvider.future);
  final users = await ref.watch(usersProvider.future);
  
  return loans.map((loan) {
    final user = users.firstWhere(
      (u) => u.id == loan.userId,
      orElse: () => UserModel(
        id: loan.userId,
        firstName: 'Desconocido',
        lastName: '',
        email: '',
        kycStatus: '',
        documentNumber: 'N/A'
      ),
    );
    return {
      'loan': loan,
      'user': user,
    };
  }).toList();
});
